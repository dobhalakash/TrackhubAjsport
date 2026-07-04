package com.dreamnest.mapper;

import com.dreamnest.dto.response.CouponResponse;
import com.dreamnest.entity.Coupon;

/**
 * Maps {@link Coupon} entities to response DTOs.
 */
public class CouponMapper {

    private CouponMapper() {
    }

    public static CouponResponse toResponse(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        CouponResponse response = new CouponResponse();
        response.setId(coupon.getId());
        response.setCode(coupon.getCode());
        response.setDiscountType(coupon.getDiscountType());
        response.setDiscountValue(coupon.getDiscountValue());
        response.setMinOrderValue(coupon.getMinOrderValue());
        response.setMaxDiscount(coupon.getMaxDiscount());
        response.setExpiryDate(coupon.getExpiryDate());
        response.setUsageLimit(coupon.getUsageLimit());
        response.setUsedCount(coupon.getUsedCount());
        response.setActive(coupon.isActive());
        return response;
    }
}
