package com.dreamnest.service;

import com.dreamnest.dto.request.MarkPayoutPaidRequest;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.PayoutResponse;
import com.dreamnest.dto.response.PayoutStatusHistoryResponse;
import com.dreamnest.dto.response.PayoutSummaryResponse;
import com.dreamnest.entity.Order;

import java.util.List;

/**
 * Manages business payouts: how much each business is owed for orders
 * fulfilled, and the admin's record of actually paying them.
 */
public interface PayoutService {

    /**
     * Creates one Payout per business involved in the order (an order can
     * contain items from multiple sellers), once that order's payment is
     * confirmed. Safe to call more than once - existing payouts for the
     * same order+business pair are left untouched.
     */
    void generatePayoutsForOrder(Order order, String razorpayPaymentId);

    PageResponse<PayoutResponse> getAllPayouts(String status, int page, int size);

    PageResponse<PayoutResponse> getPayoutsForBusiness(Long businessUserId, String status, int page, int size);

    PayoutResponse getPayoutById(Long payoutId, Long requesterId, boolean isAdmin);

    List<PayoutStatusHistoryResponse> getHistory(Long payoutId);

    /** Admin updates a payout's status, optionally with UTR/method/date/remarks/proof - all changes are logged to the audit trail. */
    PayoutResponse updateStatus(Long payoutId, MarkPayoutPaidRequest request, Long adminUserId);

    /** Business raises a dispute on a payout they believe is wrong or unpaid. */
    PayoutResponse raiseDispute(Long payoutId, Long businessUserId, String note);

    /** Summary totals (pending/paid) for a business's own payouts page. */
    PayoutSummaryResponse getSummaryForBusiness(Long businessUserId);
}
