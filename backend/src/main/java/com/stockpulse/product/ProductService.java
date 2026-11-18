package com.stockpulse.product;

import com.stockpulse.common.exception.BusinessConflictException;
import com.stockpulse.common.exception.ResourceNotFoundException;
import com.stockpulse.event.InventorySignalPublisher;
import com.stockpulse.inventory.InventorySnapshot;
import com.stockpulse.inventory.InventorySnapshotRepository;
import com.stockpulse.product.dto.CreateProductRequest;
import com.stockpulse.product.dto.OrderResponse;
import com.stockpulse.product.dto.ProductResponse;
import com.stockpulse.product.dto.SimulateOrderRequest;
import com.stockpulse.product.dto.UpdateStockRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final InventorySignalPublisher inventorySignalPublisher;
    private final double demandSpikeMultiplier;

    @Autowired
    public ProductService(ProductRepository productRepository,
                          InventorySnapshotRepository inventorySnapshotRepository,
                          InventorySignalPublisher inventorySignalPublisher,
                          @Value("${commerce.demand-spike-multiplier:3.0}") double demandSpikeMultiplier) {
        this.productRepository = productRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.inventorySignalPublisher = inventorySignalPublisher;
        this.demandSpikeMultiplier = demandSpikeMultiplier;
    }

    public ProductService(ProductRepository productRepository,
                          InventorySnapshotRepository inventorySnapshotRepository) {
        this(productRepository, inventorySnapshotRepository, null, 3.0);
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String normalizedSku = request.sku().trim();
        if (productRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new BusinessConflictException("A product with this SKU already exists");
        }

        Product product = new Product(
                normalizedSku,
                request.name().trim(),
                request.category(),
                request.currentPrice(),
                request.costPrice(),
                request.stockLevel(),
                request.reorderThreshold(),
                request.demandVelocity());
        Product saved = productRepository.save(product);
        inventorySnapshotRepository.save(new InventorySnapshot(saved));
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(ProductStatus status, ProductCategory category, Pageable pageable) {
        Page<Product> products;
        if (status != null && category != null) {
            products = productRepository.findByStatusAndCategory(status, category, pageable);
        } else if (status != null) {
            products = productRepository.findByStatus(status, pageable);
        } else if (category != null) {
            products = productRepository.findByCategory(category, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse updateStock(UUID productId, UpdateStockRequest request) {
        Product product = getProduct(productId);
        product.updateStock(request.stockLevel());
        inventorySnapshotRepository.save(new InventorySnapshot(product));
        publishSignal(product);
        return ProductResponse.from(product);
    }

    @Transactional
    public OrderResponse simulateOrder(UUID productId, SimulateOrderRequest request) {
        Product product = getProduct(productId);
        int previousStock = product.getStockLevel();
        int previousVelocity = product.getDemandVelocity();
        if (request.quantity() > previousStock) {
            throw new BusinessConflictException("Order quantity exceeds available stock");
        }

        product.recordSale(request.quantity());
        inventorySnapshotRepository.save(new InventorySnapshot(product));
        boolean recommendationsQueued = publishSignal(product);
        return new OrderResponse(
                product.getId(),
                request.quantity(),
                previousStock,
                product.getStockLevel(),
                previousVelocity,
                product.getDemandVelocity(),
                recommendationsQueued);
    }

    @Transactional(readOnly = true)
    public Product getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @Transactional(readOnly = true)
    public double categoryAverageDemand(ProductCategory category) {
        Double average = productRepository.averageDemandVelocityByCategory(category);
        return average == null ? 0.0 : average;
    }

    private boolean publishSignal(Product product) {
        if (inventorySignalPublisher == null) {
            return false;
        }
        double categoryAverageDemand = categoryAverageDemand(product.getCategory());
        return inventorySignalPublisher.publishIfTriggered(
                product, categoryAverageDemand, demandSpikeMultiplier);
    }
}