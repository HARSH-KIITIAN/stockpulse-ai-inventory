# StockPulse

StockPulse is an inventory and dynamic-pricing advisor. It observes inventory and demand signals, creates pricing and replenishment suggestions, and waits for a merchandising decision before applying side effects.

## Run locally

Requirements: Java 17, Maven 3.9+, and Node.js 18+ with npm.

The repository is intentionally split into two independently runnable modules:

```text
backend/   Spring Boot, Maven, JPA, H2
frontend/  React 18, TypeScript, Vite
```

The frontend calls the backend API through the Vite development proxy. Keep both processes running during local development. The backend is API-only; the React application is the supported console.

```bash
cd backend
mvn spring-boot:run
```

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the React merchandising console at <http://localhost:5173/>. The backend API remains available at <http://localhost:8080/>. The default storage is an in-memory H2 database and the default strategy is deterministic `RULE_BASED`. No external AI credentials are required.

To start with reproducible demo products:

```bash
cd backend
SEED_DATA_ENABLED=true mvn spring-boot:run
```

On Windows Command Prompt (`cmd.exe`), use:

```bat
cd /d "C:\Users\zycus\Desktop\AI Inventory and Dynamic Pricing\backend"
set "SEED_DATA_ENABLED=true"
mvn spring-boot:run
```

On Windows PowerShell, use:

```powershell
cd "C:\Users\zycus\Desktop\AI Inventory and Dynamic Pricing\backend"
$env:SEED_DATA_ENABLED="true"; mvn spring-boot:run
```

The `$env:...` syntax is PowerShell syntax and must not be entered in Command Prompt.

The seed operation is opt-in and idempotent by SKU. It creates products across electronics, apparel, and home categories, including a low-stock product for the recommendation walkthrough.

## Test and compile

```bash
cd backend
mvn clean test

cd ..\frontend
npm run build
```

The test suite covers validation, product and suggestion APIs, approval side effects, strategy switching, AI parsing/fallback, inventory signal boundaries, and asynchronous failure containment.

## End-to-end walkthrough

1. Start the application with `SEED_DATA_ENABLED=true` and open the console.
2. Select a product and click **Record sale**. The sale decreases stock and increases demand velocity in one transaction.
3. When stock falls below the reorder threshold, an `INVENTORY_LOW` signal is published after commit. A demand spike can be emitted at the same time when demand exceeds the configured multiplier times the category average.
4. The asynchronous handler creates pending pricing and reorder suggestions. The console polls briefly after each simulated sale and also has a manual refresh action.
5. Review trigger badges, confidence, and reasoning. Click **Accept price** to update the product price, or reject it to resolve the proposal without a price change.
6. Click **Accept reorder** to simulate inbound stock and record an inventory snapshot, or reject it to leave inventory unchanged.

Equivalent API path:

```text
POST /products
POST /products/{id}/orders
GET  /pricing-suggestions?status=PENDING
GET  /reorder-suggestions?status=PENDING
PATCH /pricing-suggestions/{id}       {"decision":"ACCEPT"}
PATCH /reorder-suggestions/{id}       {"decision":"ACCEPT"}
```

## Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `SEED_DATA_ENABLED` | `false` | Enable deterministic demo catalog seeding |
| `COMMERCE_STRATEGY` | `RULE_BASED` | Initial strategy (`RULE_BASED` or `AI`) |
| `DEMAND_SPIKE_MULTIPLIER` | `3.0` | Demand spike threshold multiplier |
| `LLM_ENABLED` | `false` | Enable the OpenAI-compatible AI gateway |
| `LLM_BASE_URL` | configured application default | Provider base URL |
| `LLM_API_KEY` | empty | Provider bearer token; never commit it |
| `LLM_MODEL` | configured application default | Provider model name |
| `LLM_PRODUCT` | `PC1` | Provider product header value |

When AI is enabled, the gateway sends requests to `/v1/chat/completions`, uses `LLM_MODEL`, sets the `product` header from `LLM_PRODUCT`, and uses the bearer token from `LLM_API_KEY`. Provider errors, timeouts, malformed responses, and unsafe values fall back to the rule-based advisor. Credentials are intentionally not stored in source, logs, or ADR files.

The runtime strategy can also be switched without restart:

```text
GET /config/commerce-strategy
PUT /config/commerce-strategy {"strategy":"AI"}
```

## Scope and limitations

- Suggestion generation is asynchronous and in-process. The bounded executor prevents unbounded local work, but there is no external queue durability or retry store.
- Suggestions are proposals; the system never applies pricing or inbound stock automatically.
- H2 is used for local demonstration. Replace the datasource configuration for a persistent deployment.
- The console uses short polling after mutations rather than SSE.