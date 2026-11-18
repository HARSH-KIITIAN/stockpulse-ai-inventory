package com.stockpulse.commerce;

import jakarta.validation.constraints.NotNull;

public record StrategyRequest(@NotNull StrategyType strategy) {
}