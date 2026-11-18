package com.stockpulse.suggestion;

import com.stockpulse.suggestion.dto.PricingSuggestionResponse;
import com.stockpulse.suggestion.dto.ReorderSuggestionResponse;
import com.stockpulse.suggestion.dto.SuggestionCreationResult;
import com.stockpulse.suggestion.dto.SuggestionDecisionRequest;
import com.stockpulse.suggestion.dto.PricingSuggestionActionResponse;
import com.stockpulse.suggestion.dto.ReorderSuggestionActionResponse;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping("/products/{productId}/suggest-pricing")
    public ResponseEntity<PricingSuggestionResponse> suggestPricing(@PathVariable UUID productId) {
        SuggestionCreationResult<PricingSuggestionResponse> result = suggestionService.suggestPricing(productId);
        if (!result.created()) {
            return ResponseEntity.ok(result.response());
        }
        return ResponseEntity.created(URI.create("/pricing-suggestions/" + result.response().id()))
                .body(result.response());
    }

    @PostMapping("/products/{productId}/suggest-reorder")
    public ResponseEntity<ReorderSuggestionResponse> suggestReorder(@PathVariable UUID productId) {
        SuggestionCreationResult<ReorderSuggestionResponse> result = suggestionService.suggestReorder(productId);
        if (!result.created()) {
            return ResponseEntity.ok(result.response());
        }
        return ResponseEntity.created(URI.create("/reorder-suggestions/" + result.response().id()))
                .body(result.response());
    }

    @PatchMapping("/pricing-suggestions/{suggestionId}")
    public PricingSuggestionActionResponse decidePricing(
            @PathVariable Long suggestionId,
            @jakarta.validation.Valid @RequestBody SuggestionDecisionRequest request) {
        return suggestionService.decidePricing(suggestionId, request);
    }

    @PatchMapping("/reorder-suggestions/{suggestionId}")
    public ReorderSuggestionActionResponse decideReorder(
            @PathVariable Long suggestionId,
            @jakarta.validation.Valid @RequestBody SuggestionDecisionRequest request) {
        return suggestionService.decideReorder(suggestionId, request);
    }

    @GetMapping("/pricing-suggestions")
    public Page<PricingSuggestionResponse> findPricingSuggestions(
            @RequestParam(required = false) SuggestionStatus status,
            @RequestParam(required = false) UUID productId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return suggestionService.findPricingSuggestions(status, productId, pageable);
    }

    @GetMapping("/reorder-suggestions")
    public Page<ReorderSuggestionResponse> findReorderSuggestions(
            @RequestParam(required = false) SuggestionStatus status,
            @RequestParam(required = false) UUID productId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return suggestionService.findReorderSuggestions(status, productId, pageable);
    }
}