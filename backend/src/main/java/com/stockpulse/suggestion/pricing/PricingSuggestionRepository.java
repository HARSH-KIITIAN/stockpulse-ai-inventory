package com.stockpulse.suggestion.pricing;

import com.stockpulse.product.Product;
import com.stockpulse.suggestion.SuggestionStatus;
import com.stockpulse.suggestion.TriggerReason;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {

    Optional<PricingSuggestion> findFirstByProductAndStatusAndTriggerReason(
            Product product, SuggestionStatus status, TriggerReason triggerReason);

    Page<PricingSuggestion> findByStatus(SuggestionStatus status, Pageable pageable);

    Page<PricingSuggestion> findByProduct_Id(UUID productId, Pageable pageable);

    Page<PricingSuggestion> findByProduct_IdAndStatus(UUID productId, SuggestionStatus status, Pageable pageable);
}