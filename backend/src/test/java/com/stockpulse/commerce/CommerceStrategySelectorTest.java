package com.stockpulse.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.stockpulse.suggestion.pricing.PricingDirection;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CommerceStrategySelectorTest {

    @Test
    void switchesActiveAdvisorWithoutRestart() {
        CommerceAdvisor rule = mock(CommerceAdvisor.class);
        CommerceAdvisor ai = mock(CommerceAdvisor.class);
        CommerceRecommendation expected = new CommerceRecommendation(
                new PricingRecommendation(new BigDecimal("10.00"), PricingDirection.HOLD,
                        new BigDecimal("0.75"), "Hold."),
                new ReorderRecommendation(1, 7, new BigDecimal("0.85"), "Replenish."));
        when(ai.recommend(null)).thenReturn(expected);

        CommerceStrategySelector selector = new CommerceStrategySelector(rule, ai, "RULE_BASED");
        assertThat(selector.getActiveStrategy()).isEqualTo(StrategyType.RULE_BASED);

        selector.setActiveStrategy(StrategyType.AI);

        assertThat(selector.getActiveStrategy()).isEqualTo(StrategyType.AI);
        assertThat(selector.recommend(null)).isSameAs(expected);
    }
}