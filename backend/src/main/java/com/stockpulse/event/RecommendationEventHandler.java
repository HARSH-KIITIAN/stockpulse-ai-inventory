package com.stockpulse.event;

import com.stockpulse.common.exception.ResourceNotFoundException;
import com.stockpulse.suggestion.AutomaticSuggestionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RecommendationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(RecommendationEventHandler.class);

    private final AutomaticSuggestionGenerator suggestionService;

    public RecommendationEventHandler(AutomaticSuggestionGenerator suggestionService) {
        this.suggestionService = suggestionService;
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InventorySignalEvent event) {
        try {
            suggestionService.createAutomaticSuggestions(event.productId(), event.triggerReasons());
        } catch (ResourceNotFoundException exception) {
            log.warn("Ignoring automatic recommendation event for missing product {}", event.productId());
        } catch (RuntimeException exception) {
            log.error("Automatic recommendation processing failed for product {}", event.productId(), exception);
        }
    }
}