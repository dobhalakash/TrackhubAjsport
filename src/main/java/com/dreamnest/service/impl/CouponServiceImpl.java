package com.dreamnest.service.impl;

import com.dreamnest.dto.request.CouponRequest;
import com.dreamnest.dto.response.CouponResponse;
import com.dreamnest.entity.Coupon;
import com.dreamnest.exception.BadRequestException;
import com.dreamnest.exception.DuplicateResourceException;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.CouponMapper;
import com.dreamnest.repository.CouponRepository;
import com.dreamnest.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CouponService}.
 */
@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(CouponMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("A coupon with this code already exists");
        }
        Coupon coupon = new Coupon();
        applyRequest(coupon, request);
        if (request.getActive() == null) {
            coupon.setActive(true);
        }
        coupon = couponRepository.save(coupon);
        return CouponMapper.toResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = findCoupon(id);
        if (!coupon.getCode().equalsIgnoreCase(request.getCode()) && couponRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("A coupon with this code already exists");
        }
        applyRequest(coupon, request);
        coupon = couponRepository.save(coupon);
        return CouponMapper.toResponse(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = findCoupon(id);
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    @Override
    public BigDecimal validateAndCalculateDiscount(String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new BadRequestException("Invalid or inactive coupon code"));

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("This coupon has expired");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("This coupon has reached its usage limit");
        }
        if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new BadRequestException("Order does not meet the minimum value for this coupon");
        }

        BigDecimal discount;
        if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return discount;
    }

    @Override
    @Transactional
    public void incrementUsage(String code) {
        couponRepository.findByCodeAndActiveTrue(code).ifPresent(coupon -> {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        });
    }

    private void applyRequest(Coupon coupon, CouponRequest request) {
        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setUsageLimit(request.getUsageLimit());
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }
    }

    private Coupon findCoupon(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    }
}
