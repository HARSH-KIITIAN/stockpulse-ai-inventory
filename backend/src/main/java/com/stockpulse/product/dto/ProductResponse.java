package com.stockpulse.product.dto;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductCategory;
import com.stockpulse.product.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        ProductCategory category,
        BigDecimal currentPrice,
        BigDecimal costPrice,
        int stockLevel,
        int reorderThreshold,
        int demandVelocity,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(), product.getSku(), product.getName(), product.getCategory(),
                product.getCurrentPrice(), product.getCostPrice(), product.getStockLevel(),
                product.getReorderThreshold(), product.getDemandVelocity(), product.getStatus(),
                product.getCreatedAt(), product.getUpdatedAt());
    }
}