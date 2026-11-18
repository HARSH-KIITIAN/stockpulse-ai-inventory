package com.stockpulse.suggestion.dto;

/** Represents a persisted suggestion and whether this request created it. */
public record SuggestionCreationResult<T>(T response, boolean created) {
}