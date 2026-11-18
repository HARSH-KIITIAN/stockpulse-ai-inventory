package com.stockpulse.suggestion.pricing;

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
import java.util.UUID;

@Entity
@Table(name = "pricing_suggestions", indexes = {
        @Index(name = "idx_pricing_suggestions_product_status", columnList = "product_id,status"),
        @Index(name = "idx_pricing_suggestions_status", columnList = "status")
})
public class PricingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "current_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "recommended_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal recommendedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PricingDirection direction;

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

    protected PricingSuggestion() {
    }

    public PricingSuggestion(Product product, BigDecimal currentPrice, BigDecimal recommendedPrice,
                              PricingDirection direction, BigDecimal confidence, String reasoning,
                              TriggerReason triggerReason) {
        this.product = requireProduct(product);
        this.currentPrice = requirePositive(currentPrice, "Current price");
        this.recommendedPrice = requirePositive(recommendedPrice, "Recommended price");
        this.direction = requireValue(direction, "Pricing direction");
        this.confidence = requireConfidence(confidence);
        this.reasoning = requireReasoning(reasoning);
        this.triggerReason = requireValue(triggerReason, "Trigger reason");
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
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getRecommendedPrice() { return recommendedPrice; }
    public PricingDirection getDirection() { return direction; }
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

    private static Product requireProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product is required");
        }
        return product;
    }

    private static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    private static BigDecimal requireConfidence(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Confidence must be between zero and one");
        }
        return value;
    }

    private static String requireReasoning(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Reasoning is required");
        }
        return value.trim();
    }

    private static <T> T requireValue(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}