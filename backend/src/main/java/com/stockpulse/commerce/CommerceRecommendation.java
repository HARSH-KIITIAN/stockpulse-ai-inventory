package com.stockpulse.commerce;

public record CommerceRecommendation(
        PricingRecommendation pricing,
        ReorderRecommendation reorder
) {
    public CommerceRecommendation {
        if (pricing == null) {
            throw new IllegalArgumentException("Pricing recommendation is required");
        }
        if (reorder == null) {
            throw new IllegalArgumentException("Reorder recommendation is required");
        }
    }
}