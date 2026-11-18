package com.stockpulse.inventory;

import com.stockpulse.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_snapshots", indexes = {
        @Index(name = "idx_inventory_snapshots_product_captured_at", columnList = "product_id,captured_at")
})
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "stock_level", nullable = false)
    private int stockLevel;

    @Column(name = "demand_velocity", nullable = false)
    private int demandVelocity;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    protected InventorySnapshot() {
    }

    public InventorySnapshot(Product product) {
        this.product = product;
        this.stockLevel = product.getStockLevel();
        this.demandVelocity = product.getDemandVelocity();
        this.capturedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getStockLevel() { return stockLevel; }
    public int getDemandVelocity() { return demandVelocity; }
    public Instant getCapturedAt() { return capturedAt; }
}