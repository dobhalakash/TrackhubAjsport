package com.dreamnest.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for applying a coupon code to the cart/checkout.
 */
public class ApplyCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    public ApplyCouponRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
