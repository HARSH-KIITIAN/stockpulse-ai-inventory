package com.stockpulse.suggestion.reorder;

import com.stockpulse.product.Product;
import com.stockpulse.suggestion.SuggestionStatus;
import com.stockpulse.suggestion.TriggerReason;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {

    Optional<ReorderSuggestion> findFirstByProductAndStatusAndTriggerReason(
            Product product, SuggestionStatus status, TriggerReason triggerReason);

    Page<ReorderSuggestion> findByStatus(SuggestionStatus status, Pageable pageable);

    Page<ReorderSuggestion> findByProduct_Id(UUID productId, Pageable pageable);

    Page<ReorderSuggestion> findByProduct_IdAndStatus(UUID productId, SuggestionStatus status, Pageable pageable);
}