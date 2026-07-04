package com.dreamnest.dto.request;

import com.dreamnest.enums.ProductSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for adding a product to the cart.
 */
public class AddCartItemRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    private ProductSize size;

    @Min(1)
    private Integer quantity = 1;

    public AddCartItemRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public ProductSize getSize() {
        return size;
    }

    public void setSize(ProductSize size) {
        this.size = size;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
