package com.stockpulse.suggestion.dto;

import com.stockpulse.suggestion.SuggestionStatus;
import com.stockpulse.suggestion.TriggerReason;
import com.stockpulse.suggestion.reorder.ReorderSuggestion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReorderSuggestionResponse(
        Long id,
        UUID productId,
        int currentStock,
        int recommendedQuantity,
        int suggestedLeadTimeDays,
        BigDecimal confidence,
        String reasoning,
        SuggestionStatus status,
        TriggerReason triggerReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReorderSuggestionResponse from(ReorderSuggestion suggestion) {
        return new ReorderSuggestionResponse(
                suggestion.getId(),
                suggestion.getProduct().getId(),
                suggestion.getCurrentStock(),
                suggestion.getRecommendedQuantity(),
                suggestion.getSuggestedLeadTimeDays(),
                suggestion.getConfidence(),
                suggestion.getReasoning(),
                suggestion.getStatus(),
                suggestion.getTriggerReason(),
                suggestion.getCreatedAt(),
                suggestion.getUpdatedAt());
    }
}