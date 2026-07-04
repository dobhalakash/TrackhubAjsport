package com.dreamnest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating the quantity or saved-for-later state of a cart item.
 */
public class UpdateCartItemRequest {

    @Min(1)
    private Integer quantity;

    private Boolean savedForLater;

    public UpdateCartItemRequest() {
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getSavedForLater() {
        return savedForLater;
    }

    public void setSavedForLater(Boolean savedForLater) {
        this.savedForLater = savedForLater;
    }
}
