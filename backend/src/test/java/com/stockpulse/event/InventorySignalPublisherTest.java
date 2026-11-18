package com.stockpulse.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductCategory;
import com.stockpulse.suggestion.TriggerReason;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class InventorySignalPublisherTest {

    private final ApplicationEventPublisher eventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final InventorySignalPublisher publisher = new InventorySignalPublisher(eventPublisher);

    @Test
    void publishesInventoryLowOnly() {
        Product product = product("SKU-LOW", 4, 10, 2);

        assertThat(publisher.publishIfTriggered(product, 2.0, 3.0)).isTrue();

        InventorySignalEvent event = publishedEvent();
        assertThat(event.triggerReasons()).containsExactly(TriggerReason.INVENTORY_LOW);
    }

    @Test
    void publishesDemandSpikeOnly() {
        Product product = product("SKU-SPIKE", 20, 10, 10);

        assertThat(publisher.publishIfTriggered(product, 2.0, 3.0)).isTrue();

        InventorySignalEvent event = publishedEvent();
        assertThat(event.triggerReasons()).containsExactly(TriggerReason.DEMAND_SPIKE);
    }

    @Test
    void publishesBothTriggersTogether() {
        Product product = product("SKU-BOTH", 4, 10, 10);

        assertThat(publisher.publishIfTriggered(product, 2.0, 3.0)).isTrue();

        assertThat(publishedEvent().triggerReasons())
                .containsExactlyInAnyOrder(TriggerReason.INVENTORY_LOW, TriggerReason.DEMAND_SPIKE);
    }

    @Test
    void doesNotPublishWhenNoTriggerIsActive() {
        Product product = product("SKU-HEALTHY", 20, 10, 2);

        assertThat(publisher.publishIfTriggered(product, 2.0, 3.0)).isFalse();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsInvalidDemandConfiguration() {
        Product product = product("SKU-INVALID", 20, 10, 2);

        assertThatThrownBy(() -> publisher.publishIfTriggered(product, 2.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void demandAtThresholdDoesNotCountAsSpike() {
        Product product = product("SKU-BOUNDARY", 20, 10, 6);

        assertThat(publisher.publishIfTriggered(product, 2.0, 3.0)).isFalse();
    }

    private InventorySignalEvent publishedEvent() {
        ArgumentCaptor<InventorySignalEvent> captor = ArgumentCaptor.forClass(InventorySignalEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private Product product(String sku, int stock, int threshold, int velocity) {
        Product product = new Product(sku, "Test Product", ProductCategory.HOME,
                new BigDecimal("20.00"), null, stock, threshold, velocity);
        try {
            var field = Product.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(product, UUID.randomUUID());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        return product;
    }
}