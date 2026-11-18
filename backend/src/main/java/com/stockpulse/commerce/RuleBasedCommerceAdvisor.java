package com.stockpulse.commerce;

import com.stockpulse.suggestion.pricing.PricingDirection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Deterministic fallback advisor. It has no external dependencies and is safe
 * to use from both request and asynchronous processing paths.
 */
@Component("ruleBasedCommerceAdvisor")
public class RuleBasedCommerceAdvisor implements CommerceAdvisor {

    private static final BigDecimal LOW_STOCK_INCREASE = new BigDecimal("1.10");
    private static final BigDecimal DEMAND_SPIKE_INCREASE = new BigDecimal("1.05");
    private static final BigDecimal DEMAND_SPIKE_MULTIPLIER = new BigDecimal("2");
    private static final BigDecimal LOW_STOCK_CONFIDENCE = new BigDecimal("0.95");
    private static final BigDecimal DEMAND_SPIKE_CONFIDENCE = new BigDecimal("0.90");
    private static final BigDecimal HOLD_CONFIDENCE = new BigDecimal("0.75");
    private static final BigDecimal REORDER_CONFIDENCE = new BigDecimal("0.85");
    private static final int DEFAULT_LEAD_TIME_DAYS = 7;

    @Override
    public CommerceRecommendation recommend(CommerceContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Commerce context is required");
        }
        return new CommerceRecommendation(
                recommendPricing(context),
                recommendReorder(context));
    }

    private PricingRecommendation recommendPricing(CommerceContext context) {
        if (context.stockLevel() < context.reorderThreshold()) {
            BigDecimal price = increase(context.currentPrice(), LOW_STOCK_INCREASE);
            return new PricingRecommendation(
                    price,
                    PricingDirection.INCREASE,
                    LOW_STOCK_CONFIDENCE,
                    "Stock for " + context.productName()
                            + " is below the reorder threshold; a modest price increase may protect remaining inventory.");
        }

        BigDecimal demandThreshold = context.categoryAverageDemand().multiply(DEMAND_SPIKE_MULTIPLIER);
        if (BigDecimal.valueOf(context.demandVelocity()).compareTo(demandThreshold) > 0) {
            BigDecimal price = increase(context.currentPrice(), DEMAND_SPIKE_INCREASE);
            return new PricingRecommendation(
                    price,
                    PricingDirection.INCREASE,
                    DEMAND_SPIKE_CONFIDENCE,
                    "Demand velocity for " + context.productName()
                            + " is more than twice the category average; a modest increase may capture demand.");
        }

        return new PricingRecommendation(
                money(context.currentPrice()),
                PricingDirection.HOLD,
                HOLD_CONFIDENCE,
                "Stock and demand velocity do not currently justify a price change.");
    }

    private ReorderRecommendation recommendReorder(CommerceContext context) {
        long targetStock;
        try {
            targetStock = Math.multiplyExact((long) context.reorderThreshold(), 3L);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Recommended reorder quantity exceeds the supported range", exception);
        }
        long quantity = Math.max(1L, targetStock - context.stockLevel());
        if (quantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Recommended reorder quantity exceeds the supported range");
        }

        return new ReorderRecommendation(
                (int) quantity,
                DEFAULT_LEAD_TIME_DAYS,
                REORDER_CONFIDENCE,
                "Replenish toward three reorder-threshold cycles to provide a practical stock buffer.");
    }

    private BigDecimal increase(BigDecimal currentPrice, BigDecimal multiplier) {
        return money(currentPrice.multiply(multiplier));
    }

    private BigDecimal money(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        if (rounded.signum() <= 0) {
            throw new IllegalArgumentException("Recommended price must be greater than zero");
        }
        return rounded;
    }
}