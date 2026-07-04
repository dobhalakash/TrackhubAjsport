package com.dreamnest.controller;

import com.dreamnest.dto.request.SendSupportMessageRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.SupportMessageResponse;
import com.dreamnest.dto.response.SupportThreadSummaryResponse;
import com.dreamnest.service.SupportMessageService;
import com.dreamnest.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Support chat between a business admin and the platform's Super Admin -
 * lets a seller raise an issue (with an optional document/image attachment)
 * and get a reply, without needing email back-and-forth.
 */
@RestController
public class SupportMessageController {

    private final SupportMessageService supportMessageService;

    public SupportMessageController(SupportMessageService supportMessageService) {
        this.supportMessageService = supportMessageService;
    }

    // ── Business admin ──────────────────────────────────────────────────

    @GetMapping("/business/support/messages")
    public ApiResponse<List<SupportMessageResponse>> getOwnThread() {
        return ApiResponse.success(supportMessageService.getThreadForBusiness(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/business/support/messages")
    public ApiResponse<SupportMessageResponse> sendAsBusiness(@Valid @RequestBody SendSupportMessageRequest request) {
        return ApiResponse.success(supportMessageService.sendAsBusiness(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/business/support/messages/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.success(supportMessageService.getUnreadCountForBusiness(SecurityUtil.getCurrentUserId()));
    }

    // ── Platform admin ───────────────────────────────────────────────────

    @GetMapping("/admin/support/messages/inbox")
    public ApiResponse<List<SupportThreadSummaryResponse>> getInbox() {
        return ApiResponse.success(supportMessageService.getInboxForAdmin());
    }

    @GetMapping("/admin/support/messages/{businessUserId}")
    public ApiResponse<List<SupportMessageResponse>> getThreadAsAdmin(@PathVariable Long businessUserId) {
        return ApiResponse.success(supportMessageService.getThreadForAdmin(businessUserId));
    }

    @PostMapping("/admin/support/messages/{businessUserId}")
    public ApiResponse<SupportMessageResponse> sendAsAdmin(@PathVariable Long businessUserId,
                                                             @Valid @RequestBody SendSupportMessageRequest request) {
        return ApiResponse.success(supportMessageService.sendAsAdmin(businessUserId, SecurityUtil.getCurrentUserId(), request));
    }
}
