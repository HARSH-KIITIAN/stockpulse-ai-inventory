package com.stockpulse.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpulse.commerce.CommerceAdvisor;
import com.stockpulse.commerce.CommerceContext;
import com.stockpulse.commerce.CommerceRecommendation;
import com.stockpulse.commerce.PricingRecommendation;
import com.stockpulse.commerce.ReorderRecommendation;
import com.stockpulse.commerce.RuleBasedCommerceAdvisor;
import com.stockpulse.suggestion.pricing.PricingDirection;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("aiCommerceAdvisor")
public class AiCommerceAdvisor implements CommerceAdvisor {

    private final LlmGateway gateway;
    private final RuleBasedCommerceAdvisor fallback;
    private final ObjectMapper objectMapper;
    private final PromptBuilder promptBuilder;

    public AiCommerceAdvisor(LlmGateway gateway, RuleBasedCommerceAdvisor fallback, ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.fallback = fallback;
        this.objectMapper = objectMapper;
        this.promptBuilder = new PromptBuilder();
    }

    @Override
    public CommerceRecommendation recommend(CommerceContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Commerce context is required");
        }
        try {
            return parseAndValidate(gateway.call(promptBuilder.build(context)), context);
        } catch (RuntimeException exception) {
            return fallback.recommend(context);
        }
    }

    private CommerceRecommendation parseAndValidate(String raw, CommerceContext context) {
        try {
            String json = stripMarkdown(raw);
            JsonNode root = objectMapper.readTree(json);
            JsonNode pricing = requiredObject(root, "pricing");
            JsonNode reorder = requiredObject(root, "reorder");

            BigDecimal recommendedPrice = decimal(pricing, "recommendedPrice");
            PricingDirection direction = PricingDirection.valueOf(text(pricing, "direction").toUpperCase());
            BigDecimal pricingConfidence = decimal(pricing, "confidence");
            String pricingReasoning = text(pricing, "reasoning");
            BigDecimal maxPrice = context.currentPrice().multiply(new BigDecimal("10"));
            BigDecimal minPrice = context.currentPrice().multiply(new BigDecimal("0.10"));
            if (recommendedPrice.signum() <= 0 || recommendedPrice.compareTo(minPrice) < 0
                    || recommendedPrice.compareTo(maxPrice) > 0) {
                throw new IllegalArgumentException("AI recommended price is outside safe bounds");
            }
            if ((direction == PricingDirection.INCREASE && recommendedPrice.compareTo(context.currentPrice()) <= 0)
                    || (direction == PricingDirection.DECREASE && recommendedPrice.compareTo(context.currentPrice()) >= 0)
                    || (direction == PricingDirection.HOLD
                    && recommendedPrice.compareTo(context.currentPrice()) != 0)) {
                throw new IllegalArgumentException("AI price direction does not match recommended price");
            }

            int quantity = integer(reorder, "recommendedQuantity");
            int leadTime = integer(reorder, "suggestedLeadTimeDays");
            BigDecimal reorderConfidence = decimal(reorder, "confidence");
            String reorderReasoning = text(reorder, "reasoning");
            if (quantity <= 0 || leadTime < 0) {
                throw new IllegalArgumentException("AI reorder values are invalid");
            }
            return new CommerceRecommendation(
                    new PricingRecommendation(recommendedPrice.setScale(2, java.math.RoundingMode.HALF_UP),
                            direction, confidence(pricingConfidence), pricingReasoning),
                    new ReorderRecommendation(quantity, leadTime, confidence(reorderConfidence), reorderReasoning));
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI response was invalid", exception);
        }
    }

    private BigDecimal confidence(BigDecimal value) {
        if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("AI confidence must be between zero and one");
        }
        return value;
    }

    private JsonNode requiredObject(JsonNode root, String name) {
        JsonNode node = root == null ? null : root.get(name);
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("AI response is missing " + name);
        }
        return node;
    }

    private BigDecimal decimal(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isNumber()) {
            throw new IllegalArgumentException("AI response field " + name + " is invalid");
        }
        return value.decimalValue();
    }

    private int integer(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.canConvertToInt() || !value.isIntegralNumber()) {
            throw new IllegalArgumentException("AI response field " + name + " is invalid");
        }
        return value.intValue();
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("AI response field " + name + " is invalid");
        }
        return value.asText().trim();
    }

    private String stripMarkdown(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("AI response is empty");
        }
        String value = raw.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?\\s*", "");
            value = value.replaceFirst("\\s*```$", "");
        }
        return value.trim();
    }
}