package com.dreamnest.controller;

import com.dreamnest.dto.request.MarkPayoutPaidRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.PayoutResponse;
import com.dreamnest.dto.response.PayoutStatusHistoryResponse;
import com.dreamnest.dto.response.PayoutSummaryResponse;
import com.dreamnest.service.PayoutService;
import com.dreamnest.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Business payout management - tracks what's owed to each seller from
 * customer payments, and lets the admin record and prove that payouts were
 * actually made (status, UTR, payment proof).
 */
@RestController
public class PayoutController {

    private final PayoutService payoutService;

    public PayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    // ── Platform admin ───────────────────────────────────────────────────

    @GetMapping("/admin/payouts")
    public ApiResponse<PageResponse<PayoutResponse>> getAllPayouts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(payoutService.getAllPayouts(status, page, size));
    }

    @GetMapping("/admin/payouts/{id}")
    public ApiResponse<PayoutResponse> getPayoutAsAdmin(@PathVariable Long id) {
        return ApiResponse.success(payoutService.getPayoutById(id, SecurityUtil.getCurrentUserId(), true));
    }

    @GetMapping("/admin/payouts/{id}/history")
    public ApiResponse<List<PayoutStatusHistoryResponse>> getHistory(@PathVariable Long id) {
        return ApiResponse.success(payoutService.getHistory(id));
    }

    @PutMapping("/admin/payouts/{id}/status")
    public ApiResponse<PayoutResponse> updateStatus(@PathVariable Long id, @RequestBody MarkPayoutPaidRequest request) {
        return ApiResponse.success("Payout updated", payoutService.updateStatus(id, request, SecurityUtil.getCurrentUserId()));
    }

    // ── Business admin ──────────────────────────────────────────────────

    @GetMapping("/business/payouts")
    public ApiResponse<PageResponse<PayoutResponse>> getOwnPayouts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(payoutService.getPayoutsForBusiness(SecurityUtil.getCurrentUserId(), status, page, size));
    }

    @GetMapping("/business/payouts/summary")
    public ApiResponse<PayoutSummaryResponse> getOwnSummary() {
        return ApiResponse.success(payoutService.getSummaryForBusiness(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/business/payouts/{id}")
    public ApiResponse<PayoutResponse> getOwnPayout(@PathVariable Long id) {
        return ApiResponse.success(payoutService.getPayoutById(id, SecurityUtil.getCurrentUserId(), false));
    }

    @GetMapping("/business/payouts/{id}/history")
    public ApiResponse<List<PayoutStatusHistoryResponse>> getOwnHistory(@PathVariable Long id) {
        // Ownership is verified by getPayoutById; re-check here so a business
        // can't enumerate other sellers' payout history by guessing IDs.
        payoutService.getPayoutById(id, SecurityUtil.getCurrentUserId(), false);
        return ApiResponse.success(payoutService.getHistory(id));
    }

    @PostMapping("/business/payouts/{id}/dispute")
    public ApiResponse<PayoutResponse> raiseDispute(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String note = body.get("note");
        return ApiResponse.success("Dispute raised - our team will review it", payoutService.raiseDispute(id, SecurityUtil.getCurrentUserId(), note));
    }
}
