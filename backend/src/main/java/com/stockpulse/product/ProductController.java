package com.stockpulse.product;

import com.stockpulse.product.dto.CreateProductRequest;
import com.stockpulse.product.dto.OrderResponse;
import com.stockpulse.product.dto.ProductResponse;
import com.stockpulse.product.dto.SimulateOrderRequest;
import com.stockpulse.product.dto.UpdateStockRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.created(URI.create("/products/" + response.id())).body(response);
    }

    @GetMapping
    public Page<ProductResponse> findAll(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) ProductCategory category,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return productService.findAll(status, category, pageable);
    }

    @PatchMapping("/{productId}/stock")
    public ProductResponse updateStock(@PathVariable UUID productId,
                                       @Valid @RequestBody UpdateStockRequest request) {
        return productService.updateStock(productId, request);
    }

    @PostMapping("/{productId}/orders")
    public OrderResponse simulateOrder(@PathVariable UUID productId,
                                       @Valid @RequestBody SimulateOrderRequest request) {
        return productService.simulateOrder(productId, request);
    }
}