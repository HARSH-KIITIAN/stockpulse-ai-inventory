package com.stockpulse.ai;

import com.stockpulse.commerce.CommerceContext;

public class PromptBuilder {

    public String build(CommerceContext context) {
        String triggerInstruction = switch (context.triggerReason()) {
            case INVENTORY_LOW -> "Inventory is below threshold. Explain whether protecting remaining stock with a modest increase is appropriate.";
            case DEMAND_SPIKE -> "Demand has spiked relative to peers. Consider capturing demand with a modest increase without overreacting.";
            default -> "This is a manual review. Balance inventory protection, demand, and a stable customer price.";
        };
        return "You are StockPulse's commerce advisor. " + triggerInstruction + "\n"
                + "Return JSON only with this exact shape: "
                + "{\"pricing\":{\"recommendedPrice\":29.99,\"direction\":\"INCREASE\",\"confidence\":0.82,\"reasoning\":\"...\"},"
                + "\"reorder\":{\"recommendedQuantity\":10,\"suggestedLeadTimeDays\":7,\"confidence\":0.78,\"reasoning\":\"...\"}}\n"
                + "Product name: " + context.productName() + "\n"
                + "Category: " + context.category() + "\n"
                + "Current price: " + context.currentPrice() + "\n"
                + "Stock: " + context.stockLevel() + "\n"
                + "Reorder threshold: " + context.reorderThreshold() + "\n"
                + "Demand velocity: " + context.demandVelocity() + "\n"
                + "Category average demand velocity: " + context.categoryAverageDemand() + "\n"
                + "Trigger: " + context.triggerReason();
    }
}