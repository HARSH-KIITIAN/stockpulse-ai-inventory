package com.stockpulse.commerce;

import com.stockpulse.product.ProductCategory;
import com.stockpulse.suggestion.TriggerReason;
import java.math.BigDecimal;

/**
 * Immutable business context passed to a commerce advisor. It deliberately
 * contains values rather than a JPA entity so the strategy remains testable
 * and independent of persistence.
 */
public record CommerceContext(
        String productName,
        ProductCategory category,
        BigDecimal currentPrice,
        int stockLevel,
        int reorderThreshold,
        int demandVelocity,
        BigDecimal categoryAverageDemand,
        TriggerReason triggerReason
) {

    public CommerceContext {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        productName = productName.trim();
        if (category == null) {
            throw new IllegalArgumentException("Product category is required");
        }
        if (currentPrice == null || currentPrice.signum() <= 0) {
            throw new IllegalArgumentException("Current price must be greater than zero");
        }
        if (stockLevel < 0) {
            throw new IllegalArgumentException("Stock level cannot be negative");
        }
        if (reorderThreshold <= 0) {
            throw new IllegalArgumentException("Reorder threshold must be greater than zero");
        }
        if (demandVelocity < 0) {
            throw new IllegalArgumentException("Demand velocity cannot be negative");
        }
        if (categoryAverageDemand == null || categoryAverageDemand.signum() < 0) {
            throw new IllegalArgumentException("Category average demand cannot be negative");
        }
        if (triggerReason == null) {
            throw new IllegalArgumentException("Trigger reason is required");
        }
    }
}