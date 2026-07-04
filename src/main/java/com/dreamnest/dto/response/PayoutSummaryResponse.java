package com.dreamnest.dto.response;

import java.math.BigDecimal;

public class PayoutSummaryResponse {

    private BigDecimal totalPending;
    private BigDecimal totalPaid;

    public PayoutSummaryResponse() {
    }

    public PayoutSummaryResponse(BigDecimal totalPending, BigDecimal totalPaid) {
        this.totalPending = totalPending;
        this.totalPaid = totalPaid;
    }

    public BigDecimal getTotalPending() {
        return totalPending;
    }

    public void setTotalPending(BigDecimal totalPending) {
        this.totalPending = totalPending;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }
}
