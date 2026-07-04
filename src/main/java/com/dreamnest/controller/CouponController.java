package com.dreamnest.controller;

import com.dreamnest.dto.request.ApplyCouponRequest;
import com.dreamnest.dto.request.CouponRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.CouponResponse;
import com.dreamnest.service.CartService;
import com.dreamnest.service.CouponService;
import com.dreamnest.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Endpoints for managing coupons (admin) and applying them to the cart (customer).
 */
@RestController
public class CouponController {

    private final CouponService couponService;
    private final CartService cartService;

    public CouponController(CouponService couponService, CartService cartService) {
        this.couponService = couponService;
        this.cartService = cartService;
    }

    @PostMapping("/cart/apply-coupon")
    public ApiResponse<Map<String, Object>> applyCoupon(@Valid @RequestBody ApplyCouponRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        BigDecimal subtotal = cartService.getCart(userId).getSubtotal();
        BigDecimal discount = couponService.validateAndCalculateDiscount(request.getCode(), subtotal);
        return ApiResponse.success("Coupon applied successfully", Map.of(
                "code", request.getCode().toUpperCase(),
                "subtotal", subtotal,
                "discount", discount,
                "total", subtotal.subtract(discount)
        ));
    }

    // ---- Admin management ----

    @GetMapping("/admin/coupons")
    public ApiResponse<List<CouponResponse>> getAllCoupons() {
        return ApiResponse.success(couponService.getAllCoupons());
    }

    @PostMapping("/admin/coupons")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CouponRequest request) {
        CouponResponse response = couponService.createCoupon(request);
        return new ResponseEntity<>(ApiResponse.success("Coupon created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/admin/coupons/{id}")
    public ApiResponse<CouponResponse> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.success("Coupon updated successfully", couponService.updateCoupon(id, request));
    }

    @DeleteMapping("/admin/coupons/{id}")
    public ApiResponse<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ApiResponse.success("Coupon deactivated successfully", null);
    }
}
