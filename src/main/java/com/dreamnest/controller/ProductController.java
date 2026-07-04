package com.dreamnest.controller;

import com.dreamnest.dto.request.ProductRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.ProductResponse;
import com.dreamnest.dto.response.ProductSummaryResponse;
import com.dreamnest.service.ProductService;
import com.dreamnest.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Endpoints for browsing the product catalog and managing products as a business admin.
 */
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ---- Public catalog ----

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductSummaryResponse>> searchProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ApiResponse.success(productService.searchProducts(categoryId, brand, minPrice, maxPrice, keyword,
                page, size, sortBy, sortDir));
    }

    @GetMapping("/products/trending")
    public ApiResponse<List<ProductSummaryResponse>> getTrendingProducts() {
        return ApiResponse.success(productService.getTrendingProducts());
    }

    /** Fast, lightweight results for the live search-as-you-type dropdown in the navbar. */
    @GetMapping("/products/suggestions")
    public ApiResponse<List<ProductSummaryResponse>> getSearchSuggestions(@RequestParam String q) {
        return ApiResponse.success(productService.getSearchSuggestions(q));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductById(id));
    }

    @GetMapping("/products/{id}/related")
    public ApiResponse<List<ProductSummaryResponse>> getRelatedProducts(@PathVariable Long id,
                                                                          @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(productService.getRelatedProducts(id, limit));
    }

    @PostMapping("/products/{id}/notify-me")
    public ApiResponse<Void> subscribeToStockAlert(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new com.dreamnest.exception.BadRequestException("Email is required");
        }
        productService.subscribeToStockAlert(id, email.trim());
        return ApiResponse.success("You'll be notified when this product is back in stock", null);
    }

    // ---- Business admin management ----

    @GetMapping("/business/products")
    public ApiResponse<PageResponse<ProductResponse>> getMyProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.success(productService.getProductsByBusiness(userId, page, size));
    }

    @PostMapping("/business/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        ProductResponse response = productService.createProduct(request, userId);
        return new ResponseEntity<>(ApiResponse.success("Product created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/business/products/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = isSuperAdmin();
        return ApiResponse.success("Product updated successfully", productService.updateProduct(id, request, userId, isAdmin));
    }

    @DeleteMapping("/business/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean isAdmin = isSuperAdmin();
        productService.deleteProduct(id, userId, isAdmin);
        return ApiResponse.success("Product deleted successfully", null);
    }

    private boolean isSuperAdmin() {
        var principal = SecurityUtil.getCurrentUser();
        return principal != null && "SUPER_ADMIN".equals(principal.getRole());
    }
}
