package com.dreamnest.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Generic "reason" payload, used for both order cancellation and return requests. */
public class OrderReasonRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
