package com.stockpulse.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SimulateOrderRequest(@NotNull @Min(1) Integer quantity) {
}