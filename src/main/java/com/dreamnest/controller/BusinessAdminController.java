package com.dreamnest.controller;

import com.dreamnest.dto.request.UpdatePaymentSettingsRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.BusinessDashboardStatsResponse;
import com.dreamnest.dto.response.BusinessProfileResponse;
import com.dreamnest.service.BusinessAdminService;
import com.dreamnest.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the business admin dashboard.
 */
@RestController
@RequestMapping("/business")
public class BusinessAdminController {

    private final BusinessAdminService businessAdminService;

    public BusinessAdminController(BusinessAdminService businessAdminService) {
        this.businessAdminService = businessAdminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<BusinessDashboardStatsResponse> getDashboard() {
        return ApiResponse.success(businessAdminService.getDashboardStats(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/profile")
    public ApiResponse<BusinessProfileResponse> getProfile() {
        return ApiResponse.success(businessAdminService.getMyProfile(SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/payment-settings")
    public ApiResponse<BusinessProfileResponse> updatePaymentSettings(@Valid @RequestBody UpdatePaymentSettingsRequest request) {
        return ApiResponse.success("Payment settings updated successfully",
                businessAdminService.updatePaymentSettings(SecurityUtil.getCurrentUserId(), request));
    }
}
