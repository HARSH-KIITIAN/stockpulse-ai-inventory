package com.stockpulse.product;

import com.stockpulse.suggestion.pricing.PricingSuggestion;
import com.stockpulse.suggestion.reorder.ReorderSuggestion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_status", columnList = "status"),
        @Index(name = "idx_products_category", columnList = "category")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @Column(name = "current_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "stock_level", nullable = false)
    private int stockLevel;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold;

    @Column(name = "demand_velocity", nullable = false)
    private int demandVelocity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "product", fetch = jakarta.persistence.FetchType.LAZY)
    private List<PricingSuggestion> pricingSuggestions = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = jakarta.persistence.FetchType.LAZY)
    private List<ReorderSuggestion> reorderSuggestions = new ArrayList<>();

    protected Product() {
    }

    public Product(String sku, String name, ProductCategory category, BigDecimal currentPrice,
                   BigDecimal costPrice, int stockLevel, int reorderThreshold, int demandVelocity) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.currentPrice = currentPrice;
        this.costPrice = costPrice;
        this.stockLevel = stockLevel;
        this.reorderThreshold = reorderThreshold;
        this.demandVelocity = demandVelocity;
        recalculateStatus();
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

    public void updateStock(int newStockLevel) {
        this.stockLevel = newStockLevel;
        recalculateStatus();
    }

    public void recordSale(int quantity) {
        this.stockLevel -= quantity;
        this.demandVelocity += quantity;
        recalculateStatus();
    }

    public void applyPrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        this.currentPrice = newPrice;
        recalculateStatus();
    }

    public void receiveStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Received stock quantity must be greater than zero");
        }
        try {
            this.stockLevel = Math.addExact(this.stockLevel, quantity);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Stock level exceeds the supported range", exception);
        }
        recalculateStatus();
    }

    /** Marks a completed pricing review without changing inventory. */
    public void resolvePriceReview() {
        if (stockLevel > 0) {
            status = ProductStatus.ACTIVE;
        } else {
            status = ProductStatus.OUT_OF_STOCK;
        }
    }

    public void recalculateStatus() {
        if (stockLevel == 0) {
            status = ProductStatus.OUT_OF_STOCK;
        } else if (stockLevel < reorderThreshold) {
            status = ProductStatus.PRICE_REVIEW_PENDING;
        } else {
            status = ProductStatus.ACTIVE;
        }
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public ProductCategory getCategory() { return category; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public int getStockLevel() { return stockLevel; }
    public int getReorderThreshold() { return reorderThreshold; }
    public int getDemandVelocity() { return demandVelocity; }
    public ProductStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<PricingSuggestion> getPricingSuggestions() { return List.copyOf(pricingSuggestions); }
    public List<ReorderSuggestion> getReorderSuggestions() { return List.copyOf(reorderSuggestions); }
}