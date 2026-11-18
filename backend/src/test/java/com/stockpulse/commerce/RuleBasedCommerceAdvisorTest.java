package com.stockpulse.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpulse.product.ProductCategory;
import com.stockpulse.suggestion.TriggerReason;
import com.stockpulse.suggestion.pricing.PricingDirection;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RuleBasedCommerceAdvisorTest {

    private final RuleBasedCommerceAdvisor advisor = new RuleBasedCommerceAdvisor();

    @Test
    void recommendsTenPercentIncreaseWhenStockIsBelowThreshold() {
        CommerceRecommendation recommendation = advisor.recommend(context(
                new BigDecimal("79.99"), 8, 15, 2, new BigDecimal("10")));

        assertThat(recommendation.pricing().recommendedPrice()).isEqualByComparingTo("87.99");
        assertThat(recommendation.pricing().direction()).isEqualTo(PricingDirection.INCREASE);
        assertThat(recommendation.pricing().confidence()).isEqualByComparingTo("0.95");
        assertThat(recommendation.pricing().reasoning()).contains("below the reorder threshold");
        assertThat(recommendation.reorder().recommendedQuantity()).isEqualTo(37);
    }

    @Test
    void prioritizesLowStockRuleWhenBothLowStockAndDemandSpikeApply() {
        CommerceRecommendation recommendation = advisor.recommend(context(
                new BigDecimal("100.00"), 4, 10, 30, new BigDecimal("10")));

        assertThat(recommendation.pricing().recommendedPrice()).isEqualByComparingTo("110.00");
        assertThat(recommendation.pricing().reasoning()).contains("below the reorder threshold");
    }

    @Test
    void recommendsFivePercentIncreaseWhenDemandIsMoreThanTwiceCategoryAverage() {
        CommerceRecommendation recommendation = advisor.recommend(context(
                new BigDecimal("40.00"), 20, 10, 21, new BigDecimal("10")));

        assertThat(recommendation.pricing().recommendedPrice()).isEqualByComparingTo("42.00");
        assertThat(recommendation.pricing().direction()).isEqualTo(PricingDirection.INCREASE);
        assertThat(recommendation.pricing().reasoning()).contains("twice the category average");
    }

    @Test
    void holdsPriceWhenNoRuleAppliesAndUsesMinimumReorderQuantity() {
        CommerceRecommendation recommendation = advisor.recommend(context(
                new BigDecimal("40.005"), 40, 10, 2, new BigDecimal("10")));

        assertThat(recommendation.pricing().recommendedPrice()).isEqualByComparingTo("40.01");
        assertThat(recommendation.pricing().direction()).isEqualTo(PricingDirection.HOLD);
        assertThat(recommendation.reorder().recommendedQuantity()).isEqualTo(1);
        assertThat(recommendation.reorder().suggestedLeadTimeDays()).isEqualTo(7);
    }

    @Test
    void treatsPositiveDemandAsSpikeWhenCategoryAverageIsZero() {
        CommerceRecommendation recommendation = advisor.recommend(context(
                new BigDecimal("20.00"), 10, 10, 1, BigDecimal.ZERO));

        assertThat(recommendation.pricing().direction()).isEqualTo(PricingDirection.INCREASE);
        assertThat(recommendation.pricing().recommendedPrice()).isEqualByComparingTo("21.00");
    }

    @Test
    void rejectsInvalidContext() {
        assertThatThrownBy(() -> new CommerceContext(
                "Lamp", ProductCategory.HOME, new BigDecimal("10.00"), 5, 0, 1,
                BigDecimal.ONE, TriggerReason.MANUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reorder threshold must be greater than zero");

        assertThatThrownBy(() -> advisor.recommend(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Commerce context is required");
    }

    @Test
    void rejectsReorderArithmeticOverflow() {
        CommerceContext context = context(
                new BigDecimal("10.00"), 0, Integer.MAX_VALUE, 0, BigDecimal.ZERO);

        assertThatThrownBy(() -> advisor.recommend(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recommended reorder quantity exceeds the supported range");
    }

    private CommerceContext context(BigDecimal price, int stock, int threshold,
                                    int velocity, BigDecimal categoryAverage) {
        return new CommerceContext(
                "Test product",
                ProductCategory.HOME,
                price,
                stock,
                threshold,
                velocity,
                categoryAverage,
                TriggerReason.MANUAL);
    }
}