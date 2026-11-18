package com.stockpulse.product.dto;

import com.stockpulse.product.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 255) String name,
        @NotNull ProductCategory category,
        @NotNull @DecimalMin(value = "0.01") BigDecimal currentPrice,
        @DecimalMin(value = "0.01", inclusive = true) BigDecimal costPrice,
        @NotNull @Min(0) Integer stockLevel,
        @NotNull @Min(1) Integer reorderThreshold,
        @NotNull @Min(0) Integer demandVelocity
) {
}