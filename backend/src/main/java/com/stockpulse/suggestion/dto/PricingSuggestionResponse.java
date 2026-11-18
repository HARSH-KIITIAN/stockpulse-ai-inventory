package com.stockpulse.suggestion.dto;

import com.stockpulse.suggestion.pricing.PricingDirection;
import com.stockpulse.suggestion.pricing.PricingSuggestion;
import com.stockpulse.suggestion.SuggestionStatus;
import com.stockpulse.suggestion.TriggerReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PricingSuggestionResponse(
        Long id,
        UUID productId,
        BigDecimal currentPrice,
        BigDecimal recommendedPrice,
        PricingDirection direction,
        BigDecimal confidence,
        String reasoning,
        SuggestionStatus status,
        TriggerReason triggerReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static PricingSuggestionResponse from(PricingSuggestion suggestion) {
        return new PricingSuggestionResponse(
                suggestion.getId(),
                suggestion.getProduct().getId(),
                suggestion.getCurrentPrice(),
                suggestion.getRecommendedPrice(),
                suggestion.getDirection(),
                suggestion.getConfidence(),
                suggestion.getReasoning(),
                suggestion.getStatus(),
                suggestion.getTriggerReason(),
                suggestion.getCreatedAt(),
                suggestion.getUpdatedAt());
    }
}