package com.stockpulse.commerce;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Resolves the active advisor for both HTTP and future asynchronous callers. */
@Component("commerceStrategySelector")
@Primary
public class CommerceStrategySelector implements CommerceAdvisor {

    private final Map<StrategyType, CommerceAdvisor> advisors;
    private final AtomicReference<StrategyType> activeStrategy;

    public CommerceStrategySelector(
            @Qualifier("ruleBasedCommerceAdvisor") CommerceAdvisor ruleBasedAdvisor,
            @Qualifier("aiCommerceAdvisor") CommerceAdvisor aiAdvisor,
            @Value("${commerce.strategy:RULE_BASED}") String configuredStrategy) {
        this.advisors = new EnumMap<>(StrategyType.class);
        this.advisors.put(StrategyType.RULE_BASED, ruleBasedAdvisor);
        this.advisors.put(StrategyType.AI, aiAdvisor);
        this.activeStrategy = new AtomicReference<>(parse(configuredStrategy));
    }

    @Override
    public CommerceRecommendation recommend(CommerceContext context) {
        return activeAdvisor().recommend(context);
    }

    public StrategyType getActiveStrategy() {
        return activeStrategy.get();
    }

    public void setActiveStrategy(StrategyType strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Commerce strategy is required");
        }
        activeStrategy.set(strategy);
    }

    private CommerceAdvisor activeAdvisor() {
        return advisors.get(activeStrategy.get());
    }

    private StrategyType parse(String value) {
        if (value == null || value.isBlank()) {
            return StrategyType.RULE_BASED;
        }
        try {
            return StrategyType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid commerce strategy configuration", exception);
        }
    }
}