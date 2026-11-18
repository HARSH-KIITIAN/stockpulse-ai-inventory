# StockPulse Architecture Decision Record

## Status

Accepted and maintained incrementally as implementation decisions are made.

## Scope

StockPulse is a reactive commerce advisor. It observes inventory and demand signals, generates pricing and replenishment recommendations, and waits for merchandising approval before applying changes. It is not a storefront, checkout, payment, competitor-scraping, or automated purchase-order system in the current scope.

---

## ADR-001: Place commerce logic in a dedicated advisor component

### Context

Pricing rules, reorder calculations, trigger interpretation, persistence, and HTTP handling have different responsibilities. The system must support both synchronous on-demand requests and future asynchronous inventory-signal handlers. A service that owns all of these concerns would become difficult to test and difficult to extend.

### Options

1. Put pricing and reorder rules directly in `ProductService`.
2. Put the rules in the `Product` JPA entity.
3. Define a dedicated `CommerceAdvisor` contract with strategy implementations.

### Decision

Use a dedicated `CommerceAdvisor` contract. The current implementation is `RuleBasedCommerceAdvisor`, which receives an immutable `CommerceContext` and returns a `CommerceRecommendation` containing separate pricing and reorder recommendations.

The advisor receives values rather than a JPA entity. This keeps the business logic independent of persistence and makes it directly unit-testable. Product services will remain responsible for product retrieval, transactions, snapshots, and persistence orchestration. Controllers will remain responsible for HTTP concerns.

The boundary is implemented in:

- `src/main/java/com/stockpulse/commerce/CommerceAdvisor.java`
- `src/main/java/com/stockpulse/commerce/CommerceContext.java`
- `src/main/java/com/stockpulse/commerce/RuleBasedCommerceAdvisor.java`

### Tradeoffs

- There are additional recommendation/context types compared with placing a few calculations in `ProductService`.
- Callers must map product state into `CommerceContext`.
- The separation gives us testability, a shared contract for HTTP and async callers, and a clean seam for AI and competitor-aware strategies.

---

## ADR-002: Use one unified advisor contract with separate recommendation outputs

### Context

Every inventory or demand signal may require both a price recommendation and a reorder recommendation. The AI implementation will eventually need to provide both outputs, while failures should be recoverable without losing the other recommendation. The system also needs one contract for on-demand endpoints and asynchronous event processing.

### Options

1. Use separate `PricingAdvisor` and `ReorderAdvisor` contracts and invoke them independently.
2. Use one `CommerceAdvisor` contract returning one combined pricing/reorder result.
3. Use one advisor contract but return an untyped map or provider-specific payload.

### Decision

Use one typed `CommerceAdvisor` contract returning `CommerceRecommendation`, with explicit `PricingRecommendation` and `ReorderRecommendation` members.

This provides one integration boundary for HTTP and asynchronous callers while preserving independent, type-safe pricing and replenishment outputs. It also lets the future AI implementation use a coordinated context while allowing pricing and reorder validation/fallback to remain separate.

The rule-based and AI implementations follow this decision. The selector is the primary advisor used by synchronous services and asynchronous event handling. AI parsing and validation remain behind the same typed boundary, with deterministic fallback on provider or response failures.

### Tradeoffs

- A caller receives both recommendations even when it only needs one.
- A combined result requires clear partial-failure handling in the AI implementation.
- A single combined LLM call can reduce latency and cost, but separate parsing and validation are still required for the two output sections.

---

## ADR-003: Use a strategy registry for runtime strategy selection

### Context

The active commerce strategy must be changeable without modifying code or restarting the application. The current rule-based strategy is the deterministic baseline. AI and future strategies such as `CompetitorAwareStrategy` must be able to implement the same contract and be selected by both HTTP and asynchronous callers.

### Options

1. Use an `if/else` or `switch` inside each service that calls an advisor.
2. Select one Spring bean through a static application property at startup.
3. Register advisors by a stable strategy key and resolve the active advisor through a central selector/registry.

### Decision

Use a central strategy selector backed by registered `CommerceAdvisor` implementations and a runtime-selected strategy key. The selector will be the only component that resolves the active advisor. Controllers, services, and event handlers will depend on the selector or a common advisor-facing abstraction rather than knowing implementation classes.

The selector and runtime configuration endpoint are implemented. The active strategy is process-local and defaults to the configured `RULE_BASED` value at startup.

This design gives Sprint 2 a seam for registering `CompetitorAwareStrategy` without changing existing callers.

### Tradeoffs

- A registry and selector add configuration and indirection.
- Invalid strategy keys require controlled configuration errors.
- Runtime switching introduces consistency concerns for requests already in progress.
- The design avoids scattered conditionals and supports the required no-restart strategy switch.

---

## ADR-004: Treat the rule-based strategy as the mandatory AI fallback

### Context

LLM calls can time out, exceed quota, return malformed JSON, or produce unsafe recommendations such as a zero price, an extreme price, or a non-positive reorder quantity. An asynchronous signal must never silently disappear because an external provider failed.

### Options

1. Return an error and create no suggestion when the LLM fails.
2. Retry indefinitely until the LLM succeeds.
3. Validate the LLM result and fall back to the deterministic rule-based advisor for invalid or unavailable AI output.

### Decision

Use the rule-based advisor as the mandatory fallback. The future AI advisor must:

1. Set bounded timeouts.
2. Parse structured output defensively.
3. Validate price, direction, confidence, reasoning, quantity, and lead time.
4. Reject malformed or unsafe output.
5. Fall back to `RuleBasedCommerceAdvisor` for the affected recommendation or complete recommendation result.

The current deterministic advisor is deliberately dependency-free and suitable for both request and asynchronous execution paths.

### Tradeoffs

- A fallback recommendation may be less context-aware than a successful AI recommendation.
- AI provider failures may be less visible without structured logging and metrics.
- Fallback logic increases implementation complexity, but it protects the core business workflow from external dependency failure.

---

## ADR-005: Use event-driven asynchronous recommendation generation

### Context

Stock updates and simulated orders must return quickly. Recommendation generation, especially future AI calls, should not block the inventory mutation request. The agentic behavior must be triggered by a meaningful stock or demand change rather than by a periodic poller.

### Options

1. Generate recommendations synchronously inside the stock/order request.
2. Run a scheduled poller that periodically scans products.
3. Publish an inventory signal event after the transaction and process it asynchronously.

### Decision

Use an application event published after successful stock/order processing and an asynchronous recommendation handler. The handler will:

- Determine whether inventory is below the reorder threshold.
- Determine whether demand exceeds the configured category multiplier.
- Use the shared commerce strategy contract.
- Persist pricing and reorder suggestions.
- Skip an existing `PENDING` suggestion for the same product, type, and trigger reason.
- Use rule-based fallback when AI fails.

The product workflow publishes signals from stock updates and simulated orders. The listener handles them after commit and asynchronously delegates to `SuggestionService`, which uses the shared advisor selector and skips duplicate pending records.

### Tradeoffs

- Suggestions are eventually consistent and may not be visible immediately in the response.
- In-process asynchronous events are simpler but less durable than an external message broker.
- Duplicate events are possible, so idempotency checks are required.
- The design satisfies the demonstration requirement: order → stock signal → pending suggestions → human approval.

---

## ADR-006: Preserve Sprint 2 extension seams and deliberately exclude unrelated scope

### Context

The assessment requires a working inventory-signal-to-approval loop within a short time window. Sprint 2 is expected to add competitor prices, margin floors, suppliers, and category-specific constraints. Building those features now would reduce time available for the core workflow.

### Options

1. Build the full competitor, supplier, margin, storefront, and purchase-order platform immediately.
2. Ignore future concerns and hard-code the current rules into controllers and database-specific logic.
3. Implement the P0 recommendation loop with explicit strategy, DTO, entity, and event seams for future capabilities.

### Decision

Choose the focused P0 implementation with explicit extension points:

- `Product.costPrice` is nullable for future margin calculations.
- `CommerceAdvisor` allows future strategies without changing callers.
- `CommerceContext` can receive competitor and margin data later.
- `PricingSuggestion` and `ReorderSuggestion` are separate entities because their approval side effects differ.
- `TriggerReason` keeps automatic and manual paths visible in the domain.
- Inventory snapshots provide a foundation for future history and analytics.

Deliberately deferred features are:

- Competitor scraping and competitor-aware pricing.
- Margin floors and category-specific caps.
- Supplier catalogs.
- Automated purchase orders.
- Customer storefront, cart, checkout, and payments.
- Authentication and authorization.
- SSE reasoning streaming.

These exclusions are scope decisions, not architectural oversights. They can be added after the P0 loop is working and tested.

### Tradeoffs

- The current system does not enforce future margin or competitor constraints.
- H2 is optimized for assessment setup speed rather than production deployment.
- In-process async processing does not provide external queue durability.
- The focused scope maximizes the chance of delivering the central business workflow and leaves clear seams for the next sprint.

---

## Decision maintenance rule

Add or update an ADR entry immediately after any material architectural decision. Future entries should preserve the same four-part structure: Context, Options, Decision, and Tradeoffs.

---

## ADR-008: Separate the React frontend from the Spring Boot backend

### Context

The approved blueprint calls for independent `/backend` and `/frontend` modules. The initial implementation served a plain JavaScript console from Spring Boot static resources, which coupled the UI build to the backend and did not provide the requested React and TypeScript structure.

### Options

1. Keep the static console under `backend/src/main/resources/static`.
2. Add a React/Vite frontend while retaining the static console.
3. Use an independently runnable React 18 + TypeScript + Vite frontend and keep the backend API-only.

### Decision

Use option 3. The frontend lives under `frontend/`, is built with Vite, and uses typed API models and component boundaries. Vite proxies API paths to the Spring Boot service at `http://localhost:8080` during development. The backend lives under `backend/` and owns the API, persistence, events, and advisor logic.

The frontend provides the required merchandising workflow: catalog loading, product metrics, status and trigger badges, simulated sale, asynchronous recommendation refresh, and human approval for pricing and reorder suggestions.

### Tradeoffs

- Local development requires two processes instead of one.
- The previous backend static console was removed so the repository has one supported frontend and no duplicate UI implementation.
- The separate build gives the repository the intended module boundaries and allows the frontend and backend to evolve independently.

---

## ADR-007: Human approval side effects, runtime strategy selection, and AI fallback

### Context

Pricing and replenishment recommendations must remain proposals until merchandising acts on them. The two approval actions have different side effects: pricing acceptance changes the live price, while reorder acceptance simulates inbound inventory. The active advisor must also be switchable without restarting the application, and AI failures must not remove the deterministic recommendation path.

### Options

1. Expose entity mutation directly from controllers and select the strategy with startup-only configuration.
2. Implement separate service-layer decision methods, a central runtime strategy selector, and an AI advisor that delegates invalid or unavailable responses to the rule-based advisor.
3. Apply high-confidence AI recommendations automatically and use the rule-based advisor only during application startup failures.

### Decision

Use transactional service methods for both decision types:

- Accepting a pricing suggestion updates `Product.currentPrice`, resolves the suggestion, and recalculates the product lifecycle state.
- Rejecting a pricing suggestion only resolves the suggestion.
- Accepting a reorder suggestion increments stock, resolves the suggestion, recalculates lifecycle state, and records an inventory snapshot.
- Rejecting a reorder suggestion has no inventory side effect.

Only `PENDING` suggestions may be resolved. Repeated decisions return a controlled `409 BUSINESS_CONFLICT` response.

Use `CommerceStrategySelector` as the primary `CommerceAdvisor`. It stores the active `StrategyType` in an atomic reference and exposes `GET`/`PUT /config/commerce-strategy`. Both current HTTP callers and future event handlers use the selector, so changing from `RULE_BASED` to `AI` does not require a restart.

Use `AiCommerceAdvisor` with distinct trigger-aware prompt text, structured JSON parsing, safe-bound validation, and `RuleBasedCommerceAdvisor` fallback for gateway failures, malformed JSON, invalid confidence, inconsistent direction, unsafe prices, or invalid reorder values. The LLM gateway is disabled by default and obtains configuration from environment-backed application properties.

### Tradeoffs

- Runtime strategy selection is process-local and is not yet persisted in a settings table; a restart returns to the configured default.
- The AI gateway currently targets an OpenAI-compatible `/v1/chat/completions` endpoint. Provider-specific adapters can be added later without changing the advisor contract.
- Rule-based fallback may be less context-aware than AI, but the approval workflow remains reliable when the external provider is unavailable.
- Human approval adds a checkpoint and eventual consistency, but prevents unreviewed prices or inventory changes from becoming live.
