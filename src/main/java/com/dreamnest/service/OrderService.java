package com.dreamnest.service;

import com.dreamnest.dto.request.CheckoutRequest;
import com.dreamnest.dto.request.UpdateOrderStatusRequest;
import com.dreamnest.dto.request.VerifyPaymentRequest;
import com.dreamnest.dto.response.OrderResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.RazorpayOrderResponse;
import com.dreamnest.dto.response.UpiPaymentInfoResponse;

/**
 * Manages order placement and lifecycle.
 */
public interface OrderService {

    OrderResponse checkout(Long userId, CheckoutRequest request, String clientIp);

    PageResponse<OrderResponse> getOrdersForUser(Long userId, int page, int size);

    OrderResponse getOrderById(Long userId, Long orderId, boolean isPrivileged);

    PageResponse<OrderResponse> getOrdersForBusiness(Long businessUserId, int page, int size);

    PageResponse<OrderResponse> getAllOrders(int page, int size);

    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long businessUserId, boolean isAdmin);

    UpiPaymentInfoResponse getUpiPaymentInfo(Long userId, Long orderId);

    OrderResponse confirmUpiPayment(Long userId, Long orderId);

    /** Creates (or reuses) a Razorpay order for the given TrackHub order, ready for Checkout. */
    RazorpayOrderResponse createRazorpayOrder(Long userId, Long orderId);

    /**
     * Verifies a Razorpay Checkout success callback server-side. Only marks
     * the order as paid if the cryptographic signature checks out against
     * our secret key - the order is NEVER trusted on the client's word alone.
     */
    OrderResponse verifyRazorpayPayment(Long userId, Long orderId, VerifyPaymentRequest request);

    /** Looks up orders by order number, customer email, or AWB/tracking number, for the support dashboard. */
    java.util.List<OrderResponse> searchForSupport(String query);

    /** Generates the invoice PDF for a delivered order. Throws if the order isn't delivered yet, or the requester doesn't own it. */
    byte[] generateInvoicePdf(Long userId, Long orderId, boolean isPrivileged);

    /** Customer self-cancels an order before it ships. Restocks items and refunds any prepaid amount. */
    OrderResponse cancelOrder(Long userId, Long orderId, String reason);

    /** Customer requests a return within the return window after delivery. */
    OrderResponse requestReturn(Long userId, Long orderId, String reason);

    /** Business/admin approves or rejects a pending return request. */
    OrderResponse decideReturn(Long orderId, Long businessUserId, boolean isAdmin, boolean approve);
}

