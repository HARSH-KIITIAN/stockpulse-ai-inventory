package com.stockpulse.suggestion.dto;

import com.stockpulse.product.dto.ProductResponse;

public record ReorderSuggestionActionResponse(
        ReorderSuggestionResponse suggestion,
        ProductResponse product
) {
}