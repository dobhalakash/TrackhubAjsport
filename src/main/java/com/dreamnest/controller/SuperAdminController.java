package com.dreamnest.controller;

import com.dreamnest.dto.request.BusinessApprovalRequest;
import com.dreamnest.dto.response.AdminDashboardStatsResponse;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.BusinessProfileResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.PaymentAdminResponse;
import com.dreamnest.dto.response.UserResponse;
import com.dreamnest.entity.Payment;
import com.dreamnest.enums.BusinessStatus;
import com.dreamnest.enums.PaymentStatus;
import com.dreamnest.enums.RoleName;
import com.dreamnest.repository.PaymentRepository;
import com.dreamnest.service.SuperAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Endpoints for the super admin dashboard: platform stats, business approvals, and user management.
 */
@RestController
@RequestMapping("/admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final PaymentRepository paymentRepository;

    public SuperAdminController(SuperAdminService superAdminService, PaymentRepository paymentRepository) {
        this.superAdminService = superAdminService;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardStatsResponse> getDashboard() {
        return ApiResponse.success(superAdminService.getDashboardStats());
    }

    @GetMapping("/businesses")
    public ApiResponse<PageResponse<BusinessProfileResponse>> getBusinesses(
            @RequestParam(required = false) BusinessStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(superAdminService.getBusinessesByStatus(status, page, size));
    }

    @PutMapping("/businesses/{id}/status")
    public ApiResponse<BusinessProfileResponse> updateBusinessStatus(@PathVariable Long id, @Valid @RequestBody BusinessApprovalRequest request) {
        return ApiResponse.success("Business status updated successfully", superAdminService.updateBusinessStatus(id, request));
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> getUsers(
            @RequestParam(defaultValue = "CUSTOMER") RoleName role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(superAdminService.getUsersByRole(role, page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<UserResponse> setUserEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        return ApiResponse.success("User status updated successfully", superAdminService.setUserEnabled(id, enabled));
    }

    // ── Payment tracking ──────────────────────────────────────────────────

    @GetMapping("/payments")
    public ApiResponse<PageResponse<PaymentAdminResponse>> getPayments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Payment> result = (status != null && !status.isBlank())
                ? paymentRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.valueOf(status.toUpperCase()), PageRequest.of(page, size))
                : paymentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));

        PageResponse<PaymentAdminResponse> response = new PageResponse<>();
        response.setContent(result.getContent().stream().map(this::toPaymentResponse).collect(java.util.stream.Collectors.toList()));
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setPageNumber(result.getNumber());
        response.setPageSize(result.getSize());
        response.setLast(result.isLast());
        return ApiResponse.success(response);
    }

    @GetMapping("/payments/summary")
    public ApiResponse<Map<String, Object>> getPaymentSummary() {
        BigDecimal totalRevenue = paymentRepository.sumSuccessful();
        Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("totalSuccessful", paymentRepository.countSuccessful());
        summary.put("totalFailed", paymentRepository.countFailed());
        summary.put("totalPending", paymentRepository.countPending());
        summary.put("totalRevenue", totalRevenue);
        return ApiResponse.success(summary);
    }

    private PaymentAdminResponse toPaymentResponse(Payment payment) {
        PaymentAdminResponse r = new PaymentAdminResponse();
        r.setId(payment.getId());
        if (payment.getOrder() != null) {
            r.setOrderId(payment.getOrder().getId());
            r.setOrderNumber(payment.getOrder().getOrderNumber());
            if (payment.getOrder().getUser() != null) {
                r.setCustomerName((payment.getOrder().getUser().getFirstName() + " " + payment.getOrder().getUser().getLastName()).trim());
                r.setCustomerEmail(payment.getOrder().getUser().getEmail());
            }
        }
        r.setPaymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
        r.setStatus(payment.getStatus() != null ? payment.getStatus().name() : null);
        r.setAmount(payment.getAmount());
        r.setCodDueAmount(payment.getCodDueAmount());
        r.setGatewayOrderId(payment.getGatewayOrderId());
        r.setGatewayPaymentId(payment.getGatewayPaymentId());
        r.setPaidAt(payment.getPaidAt());
        r.setCreatedAt(payment.getCreatedAt());
        return r;
    }
}
