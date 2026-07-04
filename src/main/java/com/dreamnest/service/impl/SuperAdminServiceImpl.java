package com.dreamnest.service.impl;

import com.dreamnest.dto.request.BusinessApprovalRequest;
import com.dreamnest.dto.response.AdminDashboardStatsResponse;
import com.dreamnest.dto.response.BusinessProfileResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.UserResponse;
import com.dreamnest.entity.BusinessProfile;
import com.dreamnest.entity.User;
import com.dreamnest.enums.BusinessStatus;
import com.dreamnest.enums.RoleName;
import com.dreamnest.exception.ResourceNotFoundException;
import com.dreamnest.mapper.BusinessMapper;
import com.dreamnest.mapper.UserMapper;
import com.dreamnest.repository.BusinessProfileRepository;
import com.dreamnest.repository.CategoryRepository;
import com.dreamnest.repository.OrderRepository;
import com.dreamnest.repository.ProductRepository;
import com.dreamnest.repository.UserRepository;
import com.dreamnest.service.SuperAdminService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Implementation of {@link SuperAdminService}.
 */
@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public SuperAdminServiceImpl(UserRepository userRepository,
                                  BusinessProfileRepository businessProfileRepository,
                                  OrderRepository orderRepository,
                                  ProductRepository productRepository,
                                  CategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        AdminDashboardStatsResponse stats = new AdminDashboardStatsResponse();

        stats.setTotalCustomers(userRepository.countByRoleName(RoleName.CUSTOMER));
        stats.setTotalBusinessAccounts(userRepository.countByRoleName(RoleName.BUSINESS_ADMIN));
        stats.setTotalUsers(stats.getTotalCustomers() + stats.getTotalBusinessAccounts()
                + userRepository.countByRoleName(RoleName.SUPER_ADMIN));
        stats.setPendingBusinessApprovals(businessProfileRepository.countByStatus(BusinessStatus.PENDING));
        stats.setTotalOrders(orderRepository.count());
        stats.setTotalProducts(productRepository.count());
        stats.setTotalCategories(categoryRepository.count());

        BigDecimal revenue = orderRepository.sumTotalRevenue();
        stats.setTotalRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BusinessProfileResponse> getBusinessesByStatus(BusinessStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status == null) {
            return PageResponse.from(businessProfileRepository.findAll(pageable).map(BusinessMapper::toResponse));
        }
        return PageResponse.from(businessProfileRepository.findByStatus(status, pageable).map(BusinessMapper::toResponse));
    }

    @Override
    @Transactional
    public BusinessProfileResponse updateBusinessStatus(Long profileId, BusinessApprovalRequest request) {
        BusinessProfile profile = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "id", profileId));
        profile.setStatus(request.getStatus());

        // If rejected/suspended, also disable the user's account
        if (request.getStatus() == BusinessStatus.REJECTED || request.getStatus() == BusinessStatus.SUSPENDED) {
            profile.getUser().setEnabled(false);
            userRepository.save(profile.getUser());
        } else if (request.getStatus() == BusinessStatus.APPROVED) {
            profile.getUser().setEnabled(true);
            userRepository.save(profile.getUser());
        }

        profile = businessProfileRepository.save(profile);
        return BusinessMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsersByRole(RoleName role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.from(userRepository.findByRoleName(role, pageable).map(UserMapper::toResponse));
    }

    @Override
    @Transactional
    public UserResponse setUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setEnabled(enabled);
        user = userRepository.save(user);
        return UserMapper.toResponse(user);
    }
}
