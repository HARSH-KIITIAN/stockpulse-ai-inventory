package com.stockpulse.commerce;

import com.stockpulse.suggestion.pricing.PricingDirection;
import java.math.BigDecimal;

public record PricingRecommendation(
        BigDecimal recommendedPrice,
        PricingDirection direction,
        BigDecimal confidence,
        String reasoning
) {
}