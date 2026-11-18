package com.stockpulse.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductCategory;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.suggestion.pricing.PricingDirection;
import com.stockpulse.suggestion.pricing.PricingSuggestion;
import com.stockpulse.suggestion.pricing.PricingSuggestionRepository;
import com.stockpulse.suggestion.reorder.ReorderSuggestion;
import com.stockpulse.suggestion.reorder.ReorderSuggestionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SuggestionPersistenceTest {

    private int productSequence;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @Test
    void persistsSuggestionsWithProductRelationshipsAndPendingDefaults() {
        Product product = productRepository.save(product());

        PricingSuggestion pricing = pricingSuggestionRepository.save(new PricingSuggestion(
                product, new BigDecimal("20.00"), new BigDecimal("22.00"),
                PricingDirection.INCREASE, new BigDecimal("0.800"), "Protect remaining stock.",
                TriggerReason.INVENTORY_LOW));
        ReorderSuggestion reorder = reorderSuggestionRepository.save(new ReorderSuggestion(
                product, 4, 26, 7, new BigDecimal("0.750"), "Restore stock buffer.",
                TriggerReason.INVENTORY_LOW));

        Optional<PricingSuggestion> pendingPricing = pricingSuggestionRepository
                .findFirstByProductAndStatusAndTriggerReason(product, SuggestionStatus.PENDING,
                        TriggerReason.INVENTORY_LOW);
        Optional<ReorderSuggestion> pendingReorder = reorderSuggestionRepository
                .findFirstByProductAndStatusAndTriggerReason(product, SuggestionStatus.PENDING,
                        TriggerReason.INVENTORY_LOW);

        assertThat(pricing.getId()).isNotNull();
        assertThat(reorder.getId()).isNotNull();
        assertThat(pendingPricing).contains(pricing);
        assertThat(pendingReorder).contains(reorder);
        assertThat(productRepository.findById(product.getId()).orElseThrow()
                .getPricingSuggestions()).hasSize(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow()
                .getReorderSuggestions()).hasSize(1);
    }

    @Test
    void rejectsInvalidPricingSuggestionValues() {
        Product product = productRepository.save(product());

        assertThatThrownBy(() -> new PricingSuggestion(
                product, BigDecimal.ZERO, new BigDecimal("22.00"), PricingDirection.INCREASE,
                new BigDecimal("0.800"), "Reason", TriggerReason.MANUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current price must be greater than zero");

        assertThatThrownBy(() -> new PricingSuggestion(
                product, new BigDecimal("20.00"), new BigDecimal("22.00"), PricingDirection.INCREASE,
                new BigDecimal("1.100"), "Reason", TriggerReason.MANUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Confidence must be between zero and one");
    }

    @Test
    void rejectsInvalidReorderSuggestionValues() {
        Product product = productRepository.save(product());

        assertThatThrownBy(() -> new ReorderSuggestion(
                product, 4, 0, 7, new BigDecimal("0.800"), "Reason", TriggerReason.MANUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recommended quantity must be greater than zero");

        assertThatThrownBy(() -> new ReorderSuggestion(
                product, 4, 10, -1, new BigDecimal("0.800"), "Reason", TriggerReason.MANUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Suggested lead time cannot be negative");
    }

    private Product product() {
        productSequence++;
        return new Product("SKU-SUGGESTION-" + productSequence, "Desk Lamp", ProductCategory.HOME,
                new BigDecimal("20.00"), null, 4, 10, 2);
    }
}