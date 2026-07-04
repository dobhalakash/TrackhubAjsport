package com.dreamnest.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response payload representing the user's shopping cart.
 */
public class CartResponse {

    private Long id;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private int totalItems;

    public CartResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CartItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }
}
