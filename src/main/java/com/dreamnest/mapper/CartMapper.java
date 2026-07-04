package com.dreamnest.mapper;

import com.dreamnest.dto.response.CartItemResponse;
import com.dreamnest.dto.response.CartResponse;
import com.dreamnest.entity.Cart;
import com.dreamnest.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps {@link Cart} entities to response DTOs.
 */
public class CartMapper {

    private CartMapper() {
    }

    public static CartResponse toResponse(Cart cart) {
        if (cart == null) {
            return null;
        }
        CartResponse response = new CartResponse();
        response.setId(cart.getId());

        List<CartItemResponse> itemResponses = cart.getItems() == null ? List.of() :
                cart.getItems().stream()
                        .filter(item -> !item.isSavedForLater())
                        .map(CartMapper::toItemResponse)
                        .collect(Collectors.toList());

        response.setItems(itemResponses);
        response.setTotalItems(itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum());

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setSubtotal(subtotal);

        return response;
    }

    public static CartItemResponse toItemResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setSize(item.getSize());
        response.setQuantity(item.getQuantity());
        response.setPriceAtAdd(item.getPriceAtAdd());
        response.setSavedForLater(item.isSavedForLater());

        if (item.getPriceAtAdd() != null && item.getQuantity() != null) {
            response.setLineTotal(item.getPriceAtAdd().multiply(BigDecimal.valueOf(item.getQuantity())));
        } else {
            response.setLineTotal(BigDecimal.ZERO);
        }

        if (item.getProduct() != null) {
            response.setProductId(item.getProduct().getId());
            response.setProductName(item.getProduct().getName());
            response.setAvailableStock(item.getProduct().getStock());
            response.setCodEnabled(item.getProduct().isCodEnabled());
            response.setCodAdvanceAmount(item.getProduct().getCodAdvanceAmount());
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                response.setProductImageUrl(item.getProduct().getImages().get(0).getImageUrl());
            }
        }

        return response;
    }
}
