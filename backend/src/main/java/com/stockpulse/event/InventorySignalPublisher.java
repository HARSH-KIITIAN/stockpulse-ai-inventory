package com.stockpulse.event;

import com.stockpulse.product.Product;
import com.stockpulse.suggestion.TriggerReason;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class InventorySignalPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public InventorySignalPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public boolean publishIfTriggered(Product product, double categoryAverageDemand,
                                      double demandSpikeMultiplier) {
        if (product == null) {
            throw new IllegalArgumentException("Product is required");
        }
        Set<TriggerReason> reasons = EnumSet.noneOf(TriggerReason.class);
        if (product.getStockLevel() < product.getReorderThreshold()) {
            reasons.add(TriggerReason.INVENTORY_LOW);
        }
        if (isDemandSpike(product, categoryAverageDemand, demandSpikeMultiplier)) {
            reasons.add(TriggerReason.DEMAND_SPIKE);
        }
        if (reasons.isEmpty()) {
            return false;
        }
        eventPublisher.publishEvent(new InventorySignalEvent(product.getId(), reasons));
        return true;
    }

    private boolean isDemandSpike(Product product, double categoryAverageDemand, double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier <= 0
                || !Double.isFinite(categoryAverageDemand) || categoryAverageDemand < 0) {
            throw new IllegalArgumentException("Demand spike configuration is invalid");
        }
        if (product.getDemandVelocity() <= 0) {
            return false;
        }
        return product.getDemandVelocity() > multiplier * categoryAverageDemand;
    }
}