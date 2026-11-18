package com.stockpulse.product.dto;

import java.util.UUID;

public record OrderResponse(
        UUID productId,
        int quantity,
        int previousStockLevel,
        int newStockLevel,
        int previousDemandVelocity,
        int newDemandVelocity,
        boolean recommendationsQueued
) {
}