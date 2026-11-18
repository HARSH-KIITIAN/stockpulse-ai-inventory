package com.stockpulse.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.commerce.CommerceContext;
import com.stockpulse.commerce.CommerceRecommendation;
import com.stockpulse.commerce.RuleBasedCommerceAdvisor;
import com.stockpulse.product.ProductCategory;
import com.stockpulse.suggestion.TriggerReason;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiCommerceAdvisorTest {

    private final LlmGateway gateway = Mockito.mock(LlmGateway.class);
    private final AiCommerceAdvisor advisor = new AiCommerceAdvisor(
            gateway, new RuleBasedCommerceAdvisor(), new ObjectMapper());

    @Test
    void parsesValidStructuredResponse() {
        when(gateway.call(Mockito.anyString())).thenReturn("""
                {"pricing":{"recommendedPrice":33.00,"direction":"INCREASE","confidence":0.82,"reasoning":"Protect stock."},
                 "reorder":{"recommendedQuantity":26,"suggestedLeadTimeDays":7,"confidence":0.78,"reasoning":"Restore buffer."}}
                """);

        CommerceRecommendation result = advisor.recommend(context());

        assertThat(result.pricing().recommendedPrice()).isEqualByComparingTo("33.00");
        assertThat(result.reorder().recommendedQuantity()).isEqualTo(26);
    }

    @Test
    void fallsBackForMalformedResponse() {
        when(gateway.call(Mockito.anyString())).thenReturn("not-json");

        CommerceRecommendation result = advisor.recommend(context());

        assertThat(result.pricing().recommendedPrice()).isEqualByComparingTo("33.00");
        assertThat(result.reorder().recommendedQuantity()).isEqualTo(26);
    }

    @Test
    void fallsBackWhenGatewayFails() {
        when(gateway.call(Mockito.anyString())).thenThrow(new UnavailableLlmException("timeout"));

        CommerceRecommendation result = advisor.recommend(context());

        assertThat(result.pricing().direction().name()).isEqualTo("INCREASE");
        assertThat(result.reorder().recommendedQuantity()).isEqualTo(26);
    }

    @Test
    void fallsBackForUnsafePrice() {
        when(gateway.call(Mockito.anyString())).thenReturn("""
                {"pricing":{"recommendedPrice":999999,"direction":"INCREASE","confidence":0.82,"reasoning":"Too high."},
                 "reorder":{"recommendedQuantity":26,"suggestedLeadTimeDays":7,"confidence":0.78,"reasoning":"Restore buffer."}}
                """);

        assertThat(advisor.recommend(context()).pricing().recommendedPrice())
                .isEqualByComparingTo("33.00");
    }

    private CommerceContext context() {
        return new CommerceContext("Desk Lamp", ProductCategory.HOME, new BigDecimal("30.00"),
                4, 10, 2, new BigDecimal("2"), TriggerReason.INVENTORY_LOW);
    }
}