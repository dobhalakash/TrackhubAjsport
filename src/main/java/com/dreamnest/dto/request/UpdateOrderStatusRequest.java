package com.dreamnest.dto.request;

import com.dreamnest.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating an order's status (business/admin only).
 */
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    public UpdateOrderStatusRequest() {
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
