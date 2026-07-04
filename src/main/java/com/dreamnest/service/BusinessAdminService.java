package com.dreamnest.service;

import com.dreamnest.dto.request.UpdatePaymentSettingsRequest;
import com.dreamnest.dto.response.BusinessDashboardStatsResponse;
import com.dreamnest.dto.response.BusinessProfileResponse;

/**
 * Provides business-admin-scoped dashboard data.
 */
public interface BusinessAdminService {

    BusinessDashboardStatsResponse getDashboardStats(Long businessUserId);

    BusinessProfileResponse getMyProfile(Long businessUserId);

    BusinessProfileResponse updatePaymentSettings(Long businessUserId, UpdatePaymentSettingsRequest request);
}
