package com.dreamnest.service.impl;

import com.dreamnest.dto.request.UpdatePaymentSettingsRequest;
import com.dreamnest.dto.response.BusinessDashboardStatsResponse;
import com.dreamnest.dto.response.BusinessProfileResponse;
import com.dreamnest.entity.BusinessProfile;
import com.dreamnest.enums.OrderStatus;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.BusinessMapper;
import com.dreamnest.repository.BusinessProfileRepository;
import com.dreamnest.repository.OrderRepository;
import com.dreamnest.repository.ProductRepository;
import com.dreamnest.service.BusinessAdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Implementation of {@link BusinessAdminService}.
 */
@Service
public class BusinessAdminServiceImpl implements BusinessAdminService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final BusinessProfileRepository businessProfileRepository;

    public BusinessAdminServiceImpl(ProductRepository productRepository,
                                     OrderRepository orderRepository,
                                     BusinessProfileRepository businessProfileRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.businessProfileRepository = businessProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessDashboardStatsResponse getDashboardStats(Long businessUserId) {
        BusinessDashboardStatsResponse stats = new BusinessDashboardStatsResponse();

        stats.setTotalProducts(productRepository.countByBusinessUserId(businessUserId));
        stats.setLowStockProducts(productRepository.countByBusinessUserIdAndStockLessThan(businessUserId, LOW_STOCK_THRESHOLD));
        stats.setTotalCategoriesUsed(productRepository.countDistinctCategoriesByBusinessUserId(businessUserId));

        long totalOrders = orderRepository.findByBusinessUserId(businessUserId, PageRequest.of(0, 1)).getTotalElements();
        stats.setTotalOrders(totalOrders);

        long pendingOrders = orderRepository.findByBusinessUserId(businessUserId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.PROCESSING)
                .count();
        stats.setPendingOrders(pendingOrders);

        BigDecimal revenue = orderRepository.sumRevenueByBusinessUser(businessUserId);
        stats.setTotalRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessProfileResponse getMyProfile(Long businessUserId) {
        BusinessProfile profile = businessProfileRepository.findByUserId(businessUserId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", businessUserId));
        return BusinessMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public BusinessProfileResponse updatePaymentSettings(Long businessUserId, UpdatePaymentSettingsRequest request) {
        BusinessProfile profile = businessProfileRepository.findByUserId(businessUserId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", businessUserId));

        String upiId = request.getUpiId();
        profile.setUpiId(upiId == null || upiId.isBlank() ? null : upiId.trim());

        if (request.getBankAccountNumber() != null) {
            profile.setBankAccountNumber(blankToNull(request.getBankAccountNumber()));
        }
        if (request.getIfscCode() != null) {
            profile.setIfscCode(blankToNull(request.getIfscCode()));
        }
        if (request.getBankName() != null) {
            profile.setBankName(blankToNull(request.getBankName()));
        }
        if (request.getAccountHolderName() != null) {
            profile.setAccountHolderName(blankToNull(request.getAccountHolderName()));
        }

        profile = businessProfileRepository.save(profile);
        return BusinessMapper.toResponse(profile);
    }

    private String blankToNull(String value) {
        return value.isBlank() ? null : value.trim();
    }
}
