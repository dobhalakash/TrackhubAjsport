package com.dreamnest.service;

import com.dreamnest.dto.request.CouponRequest;
import com.dreamnest.dto.response.CouponResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manages discount coupons.
 */
public interface CouponService {

    List<CouponResponse> getAllCoupons();

    CouponResponse createCoupon(CouponRequest request);

    CouponResponse updateCoupon(Long id, CouponRequest request);

    void deleteCoupon(Long id);

    /**
     * Validates a coupon code against the given order subtotal and returns the discount amount.
     */
    BigDecimal validateAndCalculateDiscount(String code, BigDecimal subtotal);

    /**
     * Increments the usage count for the given coupon code after a successful order.
     */
    void incrementUsage(String code);
}
