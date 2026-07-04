package com.dreamnest.service;

import com.dreamnest.dto.request.BusinessApprovalRequest;
import com.dreamnest.dto.response.AdminDashboardStatsResponse;
import com.dreamnest.dto.response.BusinessProfileResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.UserResponse;
import com.dreamnest.enums.BusinessStatus;
import com.dreamnest.enums.RoleName;

/**
 * Manages platform-wide administration: business approvals, user management, and dashboard stats.
 */
public interface SuperAdminService {

    AdminDashboardStatsResponse getDashboardStats();

    PageResponse<BusinessProfileResponse> getBusinessesByStatus(BusinessStatus status, int page, int size);

    BusinessProfileResponse updateBusinessStatus(Long profileId, BusinessApprovalRequest request);

    PageResponse<UserResponse> getUsersByRole(RoleName role, int page, int size);

    UserResponse setUserEnabled(Long userId, boolean enabled);
}
