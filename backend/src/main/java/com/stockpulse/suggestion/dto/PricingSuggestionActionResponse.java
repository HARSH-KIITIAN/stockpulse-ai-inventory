package com.stockpulse.suggestion.dto;

import com.stockpulse.product.dto.ProductResponse;

public record PricingSuggestionActionResponse(
        PricingSuggestionResponse suggestion,
        ProductResponse product
) {
}