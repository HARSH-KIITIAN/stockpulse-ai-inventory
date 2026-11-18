package com.stockpulse.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpulse.commerce.CommerceAdvisor;
import com.stockpulse.common.exception.BusinessConflictException;
import com.stockpulse.inventory.InventorySnapshotRepository;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductCategory;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.ProductStatus;
import com.stockpulse.suggestion.dto.SuggestionDecisionRequest;
import com.stockpulse.suggestion.pricing.PricingDirection;
import com.stockpulse.suggestion.pricing.PricingSuggestion;
import com.stockpulse.suggestion.pricing.PricingSuggestionRepository;
import com.stockpulse.suggestion.reorder.ReorderSuggestion;
import com.stockpulse.suggestion.reorder.ReorderSuggestionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestionDecisionServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private PricingSuggestionRepository pricingRepository;
    @Mock private ReorderSuggestionRepository reorderRepository;
    @Mock private InventorySnapshotRepository snapshotRepository;
    @Mock private CommerceAdvisor advisor;

    private SuggestionService service;

    @BeforeEach
    void setUp() {
        service = new SuggestionService(productRepository, pricingRepository, reorderRepository,
                snapshotRepository, advisor);
    }

    @Test
    void acceptingPricingUpdatesPriceAndSuggestionStatus() {
        Product product = product();
        PricingSuggestion suggestion = new PricingSuggestion(product, new BigDecimal("30.00"),
                new BigDecimal("33.00"), PricingDirection.INCREASE, new BigDecimal("0.9"),
                "Protect stock", TriggerReason.MANUAL);
        when(pricingRepository.findById(1L)).thenReturn(Optional.of(suggestion));

        var response = service.decidePricing(1L, new SuggestionDecisionRequest(SuggestionDecision.ACCEPT));

        assertThat(product.getCurrentPrice()).isEqualByComparingTo("33.00");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(response.suggestion().status()).isEqualTo(SuggestionStatus.ACCEPTED);
        assertThat(response.product().currentPrice()).isEqualByComparingTo("33.00");
    }

    @Test
    void rejectingPricingLeavesPriceUnchanged() {
        Product product = product();
        PricingSuggestion suggestion = new PricingSuggestion(product, new BigDecimal("30.00"),
                new BigDecimal("33.00"), PricingDirection.INCREASE, new BigDecimal("0.9"),
                "Protect stock", TriggerReason.MANUAL);
        when(pricingRepository.findById(1L)).thenReturn(Optional.of(suggestion));

        var response = service.decidePricing(1L, new SuggestionDecisionRequest(SuggestionDecision.REJECT));

        assertThat(product.getCurrentPrice()).isEqualByComparingTo("30.00");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(response.suggestion().status()).isEqualTo(SuggestionStatus.REJECTED);
    }

    @Test
    void acceptingReorderIncreasesStockAndRecordsSnapshot() {
        Product product = product();
        ReorderSuggestion suggestion = new ReorderSuggestion(product, 4, 26, 7,
                new BigDecimal("0.85"), "Restore stock", TriggerReason.MANUAL);
        when(reorderRepository.findById(2L)).thenReturn(Optional.of(suggestion));

        var response = service.decideReorder(2L, new SuggestionDecisionRequest(SuggestionDecision.ACCEPT));

        assertThat(product.getStockLevel()).isEqualTo(30);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(response.suggestion().status()).isEqualTo(SuggestionStatus.ACCEPTED);
        verify(snapshotRepository).save(any());
    }

    @Test
    void resolvedSuggestionCannotBeResolvedAgain() {
        Product product = product();
        PricingSuggestion suggestion = new PricingSuggestion(product, new BigDecimal("30.00"),
                new BigDecimal("33.00"), PricingDirection.INCREASE, new BigDecimal("0.9"),
                "Protect stock", TriggerReason.MANUAL);
        suggestion.reject();
        when(pricingRepository.findById(1L)).thenReturn(Optional.of(suggestion));

        assertThatThrownBy(() -> service.decidePricing(1L,
                new SuggestionDecisionRequest(SuggestionDecision.ACCEPT)))
                .isInstanceOf(BusinessConflictException.class);
    }

    private Product product() {
        return new Product("SKU-DECISION-001", "Desk Lamp", ProductCategory.HOME,
                new BigDecimal("30.00"), null, 4, 10, 2);
    }
}