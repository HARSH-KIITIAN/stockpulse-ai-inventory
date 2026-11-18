package com.stockpulse.suggestion.reorder;

import com.stockpulse.product.Product;
import com.stockpulse.suggestion.SuggestionStatus;
import com.stockpulse.suggestion.TriggerReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reorder_suggestions", indexes = {
        @Index(name = "idx_reorder_suggestions_product_status", columnList = "product_id,status"),
        @Index(name = "idx_reorder_suggestions_status", columnList = "status")
})
public class ReorderSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "current_stock", nullable = false)
    private int currentStock;

    @Column(name = "recommended_quantity", nullable = false)
    private int recommendedQuantity;

    @Column(name = "suggested_lead_time_days", nullable = false)
    private int suggestedLeadTimeDays;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SuggestionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false, length = 30)
    private TriggerReason triggerReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ReorderSuggestion() {
    }

    public ReorderSuggestion(Product product, int currentStock, int recommendedQuantity,
                              int suggestedLeadTimeDays, BigDecimal confidence, String reasoning,
                              TriggerReason triggerReason) {
        if (product == null) {
            throw new IllegalArgumentException("Product is required");
        }
        if (currentStock < 0) {
            throw new IllegalArgumentException("Current stock cannot be negative");
        }
        if (recommendedQuantity <= 0) {
            throw new IllegalArgumentException("Recommended quantity must be greater than zero");
        }
        if (suggestedLeadTimeDays < 0) {
            throw new IllegalArgumentException("Suggested lead time cannot be negative");
        }
        if (confidence == null || confidence.compareTo(BigDecimal.ZERO) < 0
                || confidence.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence must be between zero and one");
        }
        if (reasoning == null || reasoning.isBlank()) {
            throw new IllegalArgumentException("Reasoning is required");
        }
        if (triggerReason == null) {
            throw new IllegalArgumentException("Trigger reason is required");
        }

        this.product = product;
        this.currentStock = currentStock;
        this.recommendedQuantity = recommendedQuantity;
        this.suggestedLeadTimeDays = suggestedLeadTimeDays;
        this.confidence = confidence;
        this.reasoning = reasoning.trim();
        this.triggerReason = triggerReason;
        this.status = SuggestionStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getCurrentStock() { return currentStock; }
    public int getRecommendedQuantity() { return recommendedQuantity; }
    public int getSuggestedLeadTimeDays() { return suggestedLeadTimeDays; }
    public BigDecimal getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public void accept() {
        resolve(SuggestionStatus.ACCEPTED);
    }

    public void reject() {
        resolve(SuggestionStatus.REJECTED);
    }

    private void resolve(SuggestionStatus resolvedStatus) {
        if (status != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion has already been resolved");
        }
        status = resolvedStatus;
    }
}