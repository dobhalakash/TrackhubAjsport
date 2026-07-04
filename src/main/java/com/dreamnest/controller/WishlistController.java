package com.dreamnest.controller;

import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.ProductSummaryResponse;
import com.dreamnest.service.WishlistService;
import com.dreamnest.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for managing the authenticated user's wishlist.
 */
@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ApiResponse<List<ProductSummaryResponse>> getWishlist() {
        return ApiResponse.success(wishlistService.getWishlist(userId()));
    }

    @PostMapping("/{productId}")
    public ApiResponse<Void> addToWishlist(@PathVariable Long productId) {
        wishlistService.addToWishlist(userId(), productId);
        return ApiResponse.success("Added to wishlist", null);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeFromWishlist(@PathVariable Long productId) {
        wishlistService.removeFromWishlist(userId(), productId);
        return ApiResponse.success("Removed from wishlist", null);
    }

    private Long userId() {
        return SecurityUtil.getCurrentUserId();
    }
}
