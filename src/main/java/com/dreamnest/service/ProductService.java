package com.dreamnest.service;

import com.dreamnest.dto.request.ProductRequest;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.ProductResponse;
import com.dreamnest.dto.response.ProductSummaryResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manages products, including catalog search/filter and business-scoped management.
 */
public interface ProductService {

    PageResponse<ProductSummaryResponse> searchProducts(Long categoryId, String brand, BigDecimal minPrice,
                                                          BigDecimal maxPrice, String keyword,
                                                          int page, int size, String sortBy, String sortDir);

    List<ProductSummaryResponse> getTrendingProducts();

    /** Lightweight, fast results for a live search-as-you-type dropdown (capped at ~8 items). */
    List<ProductSummaryResponse> getSearchSuggestions(String query);

    ProductResponse getProductById(Long id);

    PageResponse<ProductResponse> getProductsByBusiness(Long businessUserId, int page, int size);

    ProductResponse createProduct(ProductRequest request, Long businessUserId);

    ProductResponse updateProduct(Long id, ProductRequest request, Long businessUserId, boolean isAdmin);

    void deleteProduct(Long id, Long businessUserId, boolean isAdmin);

    /** Subscribes an email to be notified once a sold-out product is restocked. */
    void subscribeToStockAlert(Long productId, String email);

    /** Other products in the same category, excluding the given product - used for "You may also like". */
    List<ProductSummaryResponse> getRelatedProducts(Long productId, int limit);
}
