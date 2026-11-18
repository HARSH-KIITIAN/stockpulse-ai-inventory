package com.stockpulse.suggestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpulse.product.Product;
import com.stockpulse.product.ProductCategory;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.suggestion.pricing.PricingDirection;
import com.stockpulse.suggestion.pricing.PricingSuggestion;
import com.stockpulse.suggestion.pricing.PricingSuggestionRepository;
import com.stockpulse.suggestion.reorder.ReorderSuggestion;
import com.stockpulse.suggestion.reorder.ReorderSuggestionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.UUID;

@DataJpaTest
class SuggestionOptimisticLockPersistenceTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private PricingSuggestionRepository pricingRepository;
    @Autowired private ReorderSuggestionRepository reorderRepository;

    @Test
    void initializesVersionForPricingAndReorderSuggestions() {
        Product product = productRepository.save(new Product("SKU-VERSION-001", "Version Lamp",
                ProductCategory.HOME, new BigDecimal("20.00"), null, 5, 10, 1));
        PricingSuggestion pricing = pricingRepository.save(new PricingSuggestion(product,
                new BigDecimal("20.00"), new BigDecimal("22.00"), PricingDirection.INCREASE,
                new BigDecimal("0.8"), "Protect stock", TriggerReason.MANUAL));
        ReorderSuggestion reorder = reorderRepository.save(new ReorderSuggestion(product, 5, 25,
                7, new BigDecimal("0.8"), "Restore buffer", TriggerReason.MANUAL));

        assertThat(pricing.getVersion()).isZero();
        assertThat(reorder.getVersion()).isZero();

        pricing.reject();
        reorder.reject();
        pricingRepository.saveAndFlush(pricing);
        reorderRepository.saveAndFlush(reorder);

        assertThat(pricing.getVersion()).isEqualTo(1);
        assertThat(reorder.getVersion()).isEqualTo(1);
    }

    @Test
    void acquiresProductWriteLockForSuggestionCreationLookup() {
        Product product = productRepository.save(new Product("SKU-LOCK-001", "Lock Lamp",
                ProductCategory.HOME, new BigDecimal("20.00"), null, 5, 10, 1));

        Product locked = productRepository.findByIdForUpdate(product.getId()).orElseThrow();

        assertThat(locked.getId()).isEqualTo(product.getId());
    }
}