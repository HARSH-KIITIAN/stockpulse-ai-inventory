package com.stockpulse.suggestion;

import com.stockpulse.commerce.CommerceAdvisor;
import com.stockpulse.commerce.CommerceContext;
import com.stockpulse.commerce.CommerceRecommendation;
import com.stockpulse.common.exception.ResourceNotFoundException;
import com.stockpulse.common.exception.BusinessConflictException;
import com.stockpulse.inventory.InventorySnapshot;
import com.stockpulse.inventory.InventorySnapshotRepository;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductCategory;
import com.stockpulse.product.ProductRepository;
import com.stockpulse.product.dto.ProductResponse;
import com.stockpulse.suggestion.dto.PricingSuggestionResponse;
import com.stockpulse.suggestion.dto.PricingSuggestionActionResponse;
import com.stockpulse.suggestion.dto.ReorderSuggestionResponse;
import com.stockpulse.suggestion.dto.ReorderSuggestionActionResponse;
import com.stockpulse.suggestion.dto.SuggestionDecisionRequest;
import com.stockpulse.suggestion.dto.SuggestionCreationResult;
import com.stockpulse.suggestion.pricing.PricingSuggestion;
import com.stockpulse.suggestion.pricing.PricingSuggestionRepository;
import com.stockpulse.suggestion.reorder.ReorderSuggestion;
import com.stockpulse.suggestion.reorder.ReorderSuggestionRepository;
import java.util.UUID;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuggestionService implements AutomaticSuggestionGenerator {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final CommerceAdvisor commerceAdvisor;

    @Autowired
    public SuggestionService(ProductRepository productRepository,
                             PricingSuggestionRepository pricingSuggestionRepository,
                             ReorderSuggestionRepository reorderSuggestionRepository,
                             InventorySnapshotRepository inventorySnapshotRepository,
                             CommerceAdvisor commerceAdvisor) {
        this.productRepository = productRepository;
        this.pricingSuggestionRepository = pricingSuggestionRepository;
        this.reorderSuggestionRepository = reorderSuggestionRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.commerceAdvisor = commerceAdvisor;
    }

    public SuggestionService(ProductRepository productRepository,
                             PricingSuggestionRepository pricingSuggestionRepository,
                             ReorderSuggestionRepository reorderSuggestionRepository,
                             CommerceAdvisor commerceAdvisor) {
        this(productRepository, pricingSuggestionRepository, reorderSuggestionRepository, null, commerceAdvisor);
    }

    @Transactional
    public SuggestionCreationResult<PricingSuggestionResponse> suggestPricing(UUID productId) {
        Product product = getProductForSuggestionWrite(productId);
        return pricingSuggestionRepository
                .findFirstByProductAndStatusAndTriggerReason(product, SuggestionStatus.PENDING, TriggerReason.MANUAL)
                .map(existing -> new SuggestionCreationResult<>(PricingSuggestionResponse.from(existing), false))
                .orElseGet(() -> {
                    CommerceRecommendation recommendation = recommend(product, TriggerReason.MANUAL);
                    PricingSuggestion pricing = new PricingSuggestion(
                            product,
                            product.getCurrentPrice(),
                            recommendation.pricing().recommendedPrice(),
                            recommendation.pricing().direction(),
                            recommendation.pricing().confidence(),
                            recommendation.pricing().reasoning(),
                            TriggerReason.MANUAL);
                    PricingSuggestion saved = pricingSuggestionRepository.save(pricing);
                    return new SuggestionCreationResult<>(PricingSuggestionResponse.from(saved), true);
                });
    }

    @Transactional
    public SuggestionCreationResult<ReorderSuggestionResponse> suggestReorder(UUID productId) {
        Product product = getProductForSuggestionWrite(productId);
        return reorderSuggestionRepository
                .findFirstByProductAndStatusAndTriggerReason(product, SuggestionStatus.PENDING, TriggerReason.MANUAL)
                .map(existing -> new SuggestionCreationResult<>(ReorderSuggestionResponse.from(existing), false))
                .orElseGet(() -> {
                    CommerceRecommendation recommendation = recommend(product, TriggerReason.MANUAL);
                    ReorderSuggestion reorder = new ReorderSuggestion(
                            product,
                            product.getStockLevel(),
                            recommendation.reorder().recommendedQuantity(),
                            recommendation.reorder().suggestedLeadTimeDays(),
                            recommendation.reorder().confidence(),
                            recommendation.reorder().reasoning(),
                            TriggerReason.MANUAL);
                    ReorderSuggestion saved = reorderSuggestionRepository.save(reorder);
                    return new SuggestionCreationResult<>(ReorderSuggestionResponse.from(saved), true);
                });
    }

    /** Creates idempotent automatic suggestions for a committed inventory signal. */
    @Transactional
    @Override
    public void createAutomaticSuggestions(UUID productId, Set<TriggerReason> triggerReasons) {
        Product product = getProductForSuggestionWrite(productId);
        for (TriggerReason triggerReason : triggerReasons) {
            if (triggerReason != TriggerReason.INVENTORY_LOW
                    && triggerReason != TriggerReason.DEMAND_SPIKE) {
                throw new IllegalArgumentException("Invalid automatic trigger reason");
            }
            if (hasPendingPricingSuggestion(product, triggerReason)
                    && hasPendingReorderSuggestion(product, triggerReason)) {
                continue;
            }
            CommerceRecommendation recommendation = recommend(product, triggerReason);
            if (!hasPendingPricingSuggestion(product, triggerReason)) {
                pricingSuggestionRepository.save(new PricingSuggestion(
                        product,
                        product.getCurrentPrice(),
                        recommendation.pricing().recommendedPrice(),
                        recommendation.pricing().direction(),
                        recommendation.pricing().confidence(),
                        recommendation.pricing().reasoning(),
                        triggerReason));
            }
            if (!hasPendingReorderSuggestion(product, triggerReason)) {
                reorderSuggestionRepository.save(new ReorderSuggestion(
                        product,
                        product.getStockLevel(),
                        recommendation.reorder().recommendedQuantity(),
                        recommendation.reorder().suggestedLeadTimeDays(),
                        recommendation.reorder().confidence(),
                        recommendation.reorder().reasoning(),
                        triggerReason));
            }
        }
    }

    private boolean hasPendingPricingSuggestion(Product product, TriggerReason triggerReason) {
        return pricingSuggestionRepository
                .findFirstByProductAndStatusAndTriggerReason(product, SuggestionStatus.PENDING, triggerReason)
                .isPresent();
    }

    private boolean hasPendingReorderSuggestion(Product product, TriggerReason triggerReason) {
        return reorderSuggestionRepository
                .findFirstByProductAndStatusAndTriggerReason(product, SuggestionStatus.PENDING, triggerReason)
                .isPresent();
    }

    @Transactional
    public PricingSuggestionActionResponse decidePricing(Long suggestionId, SuggestionDecisionRequest request) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing suggestion not found"));
        ensurePending(suggestion.getStatus());
        Product product = suggestion.getProduct();
        if (request.decision() == SuggestionDecision.ACCEPT) {
            product.applyPrice(suggestion.getRecommendedPrice());
            suggestion.accept();
        } else {
            suggestion.reject();
        }
        product.resolvePriceReview();
        return new PricingSuggestionActionResponse(
                PricingSuggestionResponse.from(suggestion),
                ProductResponse.from(product));
    }

    @Transactional
    public ReorderSuggestionActionResponse decideReorder(Long suggestionId, SuggestionDecisionRequest request) {
        ReorderSuggestion suggestion = reorderSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reorder suggestion not found"));
        ensurePending(suggestion.getStatus());
        Product product = suggestion.getProduct();
        if (request.decision() == SuggestionDecision.ACCEPT) {
            product.receiveStock(suggestion.getRecommendedQuantity());
            suggestion.accept();
            if (inventorySnapshotRepository != null) {
                inventorySnapshotRepository.save(new InventorySnapshot(product));
            }
        } else {
            suggestion.reject();
        }
        return new ReorderSuggestionActionResponse(
                ReorderSuggestionResponse.from(suggestion),
                ProductResponse.from(product));
    }

    @Transactional(readOnly = true)
    public Page<PricingSuggestionResponse> findPricingSuggestions(
            SuggestionStatus status, UUID productId, Pageable pageable) {
        Page<PricingSuggestion> suggestions;
        if (productId != null && status != null) {
            suggestions = pricingSuggestionRepository.findByProduct_IdAndStatus(productId, status, pageable);
        } else if (productId != null) {
            suggestions = pricingSuggestionRepository.findByProduct_Id(productId, pageable);
        } else if (status != null) {
            suggestions = pricingSuggestionRepository.findByStatus(status, pageable);
        } else {
            suggestions = pricingSuggestionRepository.findAll(pageable);
        }
        return suggestions.map(PricingSuggestionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReorderSuggestionResponse> findReorderSuggestions(
            SuggestionStatus status, UUID productId, Pageable pageable) {
        Page<ReorderSuggestion> suggestions;
        if (productId != null && status != null) {
            suggestions = reorderSuggestionRepository.findByProduct_IdAndStatus(productId, status, pageable);
        } else if (productId != null) {
            suggestions = reorderSuggestionRepository.findByProduct_Id(productId, pageable);
        } else if (status != null) {
            suggestions = reorderSuggestionRepository.findByStatus(status, pageable);
        } else {
            suggestions = reorderSuggestionRepository.findAll(pageable);
        }
        return suggestions.map(ReorderSuggestionResponse::from);
    }

    private CommerceRecommendation recommend(Product product, TriggerReason triggerReason) {
        Double averageDemand = productRepository.averageDemandVelocityByCategory(product.getCategory());
        return commerceAdvisor.recommend(new CommerceContext(
                product.getName(),
                product.getCategory(),
                product.getCurrentPrice(),
                product.getStockLevel(),
                product.getReorderThreshold(),
                product.getDemandVelocity(),
                averageDemand == null ? java.math.BigDecimal.ZERO : java.math.BigDecimal.valueOf(averageDemand),
                triggerReason));
    }

    private Product getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Product getProductForSuggestionWrite(UUID productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void ensurePending(SuggestionStatus status) {
        if (status != SuggestionStatus.PENDING) {
            throw new BusinessConflictException("Suggestion has already been resolved");
        }
    }
}