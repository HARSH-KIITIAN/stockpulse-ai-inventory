package com.stockpulse.event;

import com.stockpulse.suggestion.TriggerReason;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record InventorySignalEvent(UUID productId, Set<TriggerReason> triggerReasons) {

    public InventorySignalEvent {
        if (productId == null) {
            throw new IllegalArgumentException("Product id is required");
        }
        if (triggerReasons == null || triggerReasons.isEmpty()
                || triggerReasons.stream().anyMatch(reason -> reason == null
                || (reason != TriggerReason.INVENTORY_LOW && reason != TriggerReason.DEMAND_SPIKE))) {
            throw new IllegalArgumentException("At least one valid automatic trigger reason is required");
        }
        triggerReasons = Set.copyOf(EnumSet.copyOf(triggerReasons));
    }
}