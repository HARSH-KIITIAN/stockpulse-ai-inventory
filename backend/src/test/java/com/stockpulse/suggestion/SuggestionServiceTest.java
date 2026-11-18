package com.stockpulse.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpulse.commerce.CommerceAdvisor;
import com.stockpulse.commerce.CommerceContext;
import com.stockpulse.commerce.CommerceRecommendation;
import com.stockpulse.commerce.PricingRecommendation;
import com.stockpulse.commerce.ReorderRecommendation;
import com.stockpulse.common.exception.ResourceNotFoundException;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductCategory;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.suggestion.pricing.PricingDirection;
import com.stockpulse.suggestion.pricing.PricingSuggestionRepository;
import com.stockpulse.suggestion.reorder.ReorderSuggestionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private PricingSuggestionRepository pricingRepository;
    @Mock private ReorderSuggestionRepository reorderRepository;
    @Mock private CommerceAdvisor advisor;

    private SuggestionService service;

    @BeforeEach
    void setUp() {
        service = new SuggestionService(productRepository, pricingRepository, reorderRepository, advisor);
    }

    @Test
    void createsManualPricingSuggestionUsingAdvisorOutput() {
        UUID productId = UUID.randomUUID();
        Product product = product();
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        when(productRepository.averageDemandVelocityByCategory(ProductCategory.HOME)).thenReturn(2.0);
        when(pricingRepository.findFirstByProductAndStatusAndTriggerReason(
                product, SuggestionStatus.PENDING, TriggerReason.MANUAL)).thenReturn(Optional.empty());
        when(advisor.recommend(any(CommerceContext.class))).thenReturn(recommendation());
        when(pricingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.suggestPricing(productId);

        assertThat(result.created()).isTrue();
        assertThat(result.response().recommendedPrice()).isEqualByComparingTo("33.00");
        assertThat(result.response().triggerReason()).isEqualTo(TriggerReason.MANUAL);
        ArgumentCaptor<CommerceContext> context = ArgumentCaptor.forClass(CommerceContext.class);
        verify(advisor).recommend(context.capture());
        assertThat(context.getValue().categoryAverageDemand()).isEqualByComparingTo("2.0");
    }

    @Test
    void returnsExistingPendingManualPricingSuggestionWithoutCallingAdvisor() {
        UUID productId = UUID.randomUUID();
        Product product = product();
        var existing = new com.stockpulse.suggestion.pricing.PricingSuggestion(
                product, new BigDecimal("30.00"), new BigDecimal("33.00"), PricingDirection.INCREASE,
                new BigDecimal("0.900"), "Protect stock.", TriggerReason.MANUAL);
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        when(pricingRepository.findFirstByProductAndStatusAndTriggerReason(
                product, SuggestionStatus.PENDING, TriggerReason.MANUAL)).thenReturn(Optional.of(existing));

        var result = service.suggestPricing(productId);

        assertThat(result.created()).isFalse();
        assertThat(result.response().recommendedPrice()).isEqualByComparingTo("33.00");
        verify(advisor, never()).recommend(any());
        verify(pricingRepository, never()).save(any());
    }

    @Test
    void createsManualReorderSuggestionUsingAdvisorOutput() {
        UUID productId = UUID.randomUUID();
        Product product = product();
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));
        when(productRepository.averageDemandVelocityByCategory(ProductCategory.HOME)).thenReturn(2.0);
        when(reorderRepository.findFirstByProductAndStatusAndTriggerReason(
                product, SuggestionStatus.PENDING, TriggerReason.MANUAL)).thenReturn(Optional.empty());
        when(advisor.recommend(any(CommerceContext.class))).thenReturn(recommendation());
        when(reorderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.suggestReorder(productId);

        assertThat(result.created()).isTrue();
        assertThat(result.response().recommendedQuantity()).isEqualTo(20);
        assertThat(result.response().suggestedLeadTimeDays()).isEqualTo(7);
    }

    @Test
    void rejectsUnknownProduct() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suggestPricing(productId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Product product() {
        return new Product("SKU-SERVICE-001", "Desk Lamp", ProductCategory.HOME,
                new BigDecimal("30.00"), null, 4, 10, 2);
    }

    private CommerceRecommendation recommendation() {
        return new CommerceRecommendation(
                new PricingRecommendation(new BigDecimal("33.00"), PricingDirection.INCREASE,
                        new BigDecimal("0.900"), "Protect remaining stock."),
                new ReorderRecommendation(20, 7, new BigDecimal("0.850"), "Restore stock buffer."));
    }
}