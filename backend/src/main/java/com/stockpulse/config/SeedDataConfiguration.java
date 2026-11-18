package com.stockpulse.config;

import com.stockpulse.inventory.InventorySnapshot;
import com.stockpulse.inventory.InventorySnapshotRepository;
import com.stockpulse.product.Product;
import com.stockpulse.product.ProductCategory;
import com.stockpulse.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds a small deterministic demo catalog when explicitly enabled. */
@Component
public class SeedDataConfiguration implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final boolean enabled;

    public SeedDataConfiguration(ProductRepository productRepository,
                                 InventorySnapshotRepository inventorySnapshotRepository,
                                 @Value("${stockpulse.seed.enabled:false}") boolean enabled) {
        this.productRepository = productRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        for (SeedProduct seed : seedProducts()) {
            if (productRepository.existsBySkuIgnoreCase(seed.sku())) {
                continue;
            }
            Product product = productRepository.save(new Product(
                    seed.sku(), seed.name(), seed.category(), seed.price(), seed.costPrice(),
                    seed.stock(), seed.reorderThreshold(), seed.demandVelocity()));
            inventorySnapshotRepository.save(new InventorySnapshot(product));
        }
    }

    private List<SeedProduct> seedProducts() {
        return List.of(
                new SeedProduct("DEMO-EARBUDS-001", "Wireless Earbuds Pro", ProductCategory.ELECTRONICS,
                        new BigDecimal("79.99"), new BigDecimal("42.00"), 12, 10, 5),
                new SeedProduct("DEMO-HUB-001", "USB-C Hub 7-Port", ProductCategory.ELECTRONICS,
                        new BigDecimal("34.99"), new BigDecimal("18.00"), 45, 15, 4),
                new SeedProduct("DEMO-TSHIRT-001", "Organic Cotton T-Shirt", ProductCategory.APPAREL,
                        new BigDecimal("24.99"), new BigDecimal("10.00"), 8, 15, 3),
                new SeedProduct("DEMO-SHORTS-001", "Running Shorts - Navy", ProductCategory.APPAREL,
                        new BigDecimal("39.99"), new BigDecimal("17.00"), 32, 12, 2),
                new SeedProduct("DEMO-POUR-OVER-001", "Ceramic Pour-Over Set", ProductCategory.HOME,
                        new BigDecimal("49.99"), new BigDecimal("22.00"), 20, 10, 2));
    }

    private record SeedProduct(String sku, String name, ProductCategory category, BigDecimal price,
                               BigDecimal costPrice, int stock, int reorderThreshold, int demandVelocity) {
    }
}