package com.dreamnest.dto.request;

import com.dreamnest.enums.BusinessStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for a super admin to approve or reject a business account.
 */
public class BusinessApprovalRequest {

    @NotNull(message = "Status is required")
    private BusinessStatus status;

    public BusinessApprovalRequest() {
    }

    public BusinessStatus getStatus() {
        return status;
    }

    public void setStatus(BusinessStatus status) {
        this.status = status;
    }
}
