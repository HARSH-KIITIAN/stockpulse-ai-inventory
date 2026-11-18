package com.stockpulse.suggestion.dto;

import com.stockpulse.suggestion.SuggestionDecision;
import jakarta.validation.constraints.NotNull;

public record SuggestionDecisionRequest(@NotNull SuggestionDecision decision) {
}