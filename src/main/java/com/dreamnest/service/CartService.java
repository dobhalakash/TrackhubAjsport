package com.dreamnest.service;

import com.dreamnest.dto.request.AddCartItemRequest;
import com.dreamnest.dto.request.MergeCartRequest;
import com.dreamnest.dto.request.UpdateCartItemRequest;
import com.dreamnest.dto.response.CartItemResponse;
import com.dreamnest.dto.response.CartResponse;

import java.util.List;

/**
 * Manages the authenticated user's shopping cart, including guest-cart merging on login.
 */
public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addItem(Long userId, AddCartItemRequest request);

    CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(Long userId, Long itemId);

    void clearCart(Long userId);

    CartResponse mergeCart(Long userId, MergeCartRequest request);

    List<CartItemResponse> getSavedForLater(Long userId);
}
