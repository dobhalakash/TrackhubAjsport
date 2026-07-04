package com.dreamnest.controller;

import com.dreamnest.dto.request.AddCartItemRequest;
import com.dreamnest.dto.request.MergeCartRequest;
import com.dreamnest.dto.request.UpdateCartItemRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.CartItemResponse;
import com.dreamnest.dto.response.CartResponse;
import com.dreamnest.service.CartService;
import com.dreamnest.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for managing the authenticated user's shopping cart.
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart() {
        return ApiResponse.success(cartService.getCart(userId()));
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success("Item added to cart", cartService.addItem(userId(), request));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(@PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success("Cart updated", cartService.updateItem(userId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long itemId) {
        return ApiResponse.success("Item removed from cart", cartService.removeItem(userId(), itemId));
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart() {
        cartService.clearCart(userId());
        return ApiResponse.success("Cart cleared", null);
    }

    @PostMapping("/merge")
    public ApiResponse<CartResponse> mergeCart(@RequestBody MergeCartRequest request) {
        return ApiResponse.success("Cart merged", cartService.mergeCart(userId(), request));
    }

    @GetMapping("/saved-for-later")
    public ApiResponse<List<CartItemResponse>> getSavedForLater() {
        return ApiResponse.success(cartService.getSavedForLater(userId()));
    }

    private Long userId() {
        return SecurityUtil.getCurrentUserId();
    }
}
