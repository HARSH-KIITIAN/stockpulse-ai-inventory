package com.stockpulse.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import com.stockpulse.common.exception.ResourceNotFoundException;
import com.stockpulse.suggestion.AutomaticSuggestionGenerator;
import com.stockpulse.suggestion.TriggerReason;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationEventHandlerTest {

    private final UUID productId = UUID.randomUUID();

    @Test
    void delegatesBothTriggersToSuggestionService() {
        RecordingSuggestionService suggestionService = new RecordingSuggestionService();
        RecommendationEventHandler handler = new RecommendationEventHandler(suggestionService);
        InventorySignalEvent event = new InventorySignalEvent(productId,
                EnumSet.of(TriggerReason.INVENTORY_LOW, TriggerReason.DEMAND_SPIKE));

        handler.handle(event);

        org.assertj.core.api.Assertions.assertThat(suggestionService.productId).isEqualTo(productId);
        org.assertj.core.api.Assertions.assertThat(suggestionService.triggerReasons)
                .containsExactlyInAnyOrder(TriggerReason.INVENTORY_LOW, TriggerReason.DEMAND_SPIKE);
    }

    @Test
    void ignoresMissingProductWithoutThrowing() {
        ThrowingSuggestionService suggestionService = new ThrowingSuggestionService(
                new ResourceNotFoundException("Product not found"));
        RecommendationEventHandler handler = new RecommendationEventHandler(suggestionService);

        assertThatCode(() -> handler.handle(new InventorySignalEvent(productId,
                EnumSet.of(TriggerReason.INVENTORY_LOW)))).doesNotThrowAnyException();
    }

    @Test
    void containsUnexpectedAsyncFailure() {
        ThrowingSuggestionService suggestionService = new ThrowingSuggestionService(
                new IllegalStateException("advisor unavailable"));
        RecommendationEventHandler handler = new RecommendationEventHandler(suggestionService);

        assertThatCode(() -> handler.handle(new InventorySignalEvent(productId,
                EnumSet.of(TriggerReason.DEMAND_SPIKE)))).doesNotThrowAnyException();
    }

    private static class RecordingSuggestionService implements AutomaticSuggestionGenerator {
        private UUID productId;
        private Set<TriggerReason> triggerReasons;

        @Override
        public void createAutomaticSuggestions(UUID productId, Set<TriggerReason> triggerReasons) {
            this.productId = productId;
            this.triggerReasons = triggerReasons;
        }
    }

    private static class ThrowingSuggestionService implements AutomaticSuggestionGenerator {
        private final RuntimeException failure;

        private ThrowingSuggestionService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void createAutomaticSuggestions(UUID productId, Set<TriggerReason> triggerReasons) {
            throw failure;
        }
    }
}