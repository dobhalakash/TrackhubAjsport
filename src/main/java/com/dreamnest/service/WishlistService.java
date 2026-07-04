package com.dreamnest.service;

import com.dreamnest.dto.response.ProductSummaryResponse;

import java.util.List;

/**
 * Manages a user's wishlist of products.
 */
public interface WishlistService {

    List<ProductSummaryResponse> getWishlist(Long userId);

    void addToWishlist(Long userId, Long productId);

    void removeFromWishlist(Long userId, Long productId);
}
