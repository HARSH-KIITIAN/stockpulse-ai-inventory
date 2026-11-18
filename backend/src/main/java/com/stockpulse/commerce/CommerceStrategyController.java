package com.stockpulse.commerce;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config/commerce-strategy")
public class CommerceStrategyController {

    private final CommerceStrategySelector strategySelector;

    public CommerceStrategyController(CommerceStrategySelector strategySelector) {
        this.strategySelector = strategySelector;
    }

    @GetMapping
    public StrategyResponse getStrategy() {
        return new StrategyResponse(strategySelector.getActiveStrategy());
    }

    @PutMapping
    public StrategyResponse setStrategy(@Valid @RequestBody StrategyRequest request) {
        strategySelector.setActiveStrategy(request.strategy());
        return getStrategy();
    }
}