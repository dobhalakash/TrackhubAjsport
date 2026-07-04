package com.dreamnest.dto.request;

import java.util.List;

/**
 * Request payload for merging a guest (localStorage) cart into the user's DB cart on login.
 */
public class MergeCartRequest {

    private List<AddCartItemRequest> items;

    public MergeCartRequest() {
    }

    public List<AddCartItemRequest> getItems() {
        return items;
    }

    public void setItems(List<AddCartItemRequest> items) {
        this.items = items;
    }
}
