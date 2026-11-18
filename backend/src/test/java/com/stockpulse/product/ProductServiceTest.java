package com.stockpulse.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpulse.common.exception.BusinessConflictException;
import com.stockpulse.inventory.InventorySnapshot;
import com.stockpulse.inventory.InventorySnapshotRepository;
import com.stockpulse.product.dto.CreateProductRequest;
import com.stockpulse.product.dto.OrderResponse;
import com.stockpulse.product.dto.SimulateOrderRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventorySnapshotRepository inventorySnapshotRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, inventorySnapshotRepository);
    }

    @Test
    void createStoresProductAndInitialSnapshot() {
        CreateProductRequest request = new CreateProductRequest(
                " SKU-001 ", "Desk Lamp", ProductCategory.HOME, new BigDecimal("29.99"),
                null, 4, 10, 2);
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.create(request);

        assertThat(response.sku()).isEqualTo("SKU-001");
        assertThat(response.status()).isEqualTo(ProductStatus.PRICE_REVIEW_PENDING);
        verify(inventorySnapshotRepository).save(any(InventorySnapshot.class));
    }

    @Test
    void simulateOrderDecrementsStockAndIncrementsVelocity() {
        UUID id = UUID.randomUUID();
        Product product = new Product("SKU-001", "Desk Lamp", ProductCategory.HOME,
                new BigDecimal("29.99"), null, 4, 10, 2);
        when(productRepository.findById(id)).thenReturn(java.util.Optional.of(product));

        OrderResponse response = productService.simulateOrder(id, new SimulateOrderRequest(2));

        assertThat(response.previousStockLevel()).isEqualTo(4);
        assertThat(response.newStockLevel()).isEqualTo(2);
        assertThat(response.previousDemandVelocity()).isEqualTo(2);
        assertThat(response.newDemandVelocity()).isEqualTo(4);
        verify(inventorySnapshotRepository).save(any(InventorySnapshot.class));
    }

    @Test
    void simulateOrderRejectsQuantityAboveStock() {
        UUID id = UUID.randomUUID();
        Product product = new Product("SKU-001", "Desk Lamp", ProductCategory.HOME,
                new BigDecimal("29.99"), null, 1, 10, 2);
        when(productRepository.findById(id)).thenReturn(java.util.Optional.of(product));

        assertThatThrownBy(() -> productService.simulateOrder(id, new SimulateOrderRequest(2)))
                .isInstanceOf(BusinessConflictException.class);
    }
}