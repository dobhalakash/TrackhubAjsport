package com.dreamnest.controller;

import com.dreamnest.dto.request.CheckoutRequest;
import com.dreamnest.dto.request.OrderReasonRequest;
import com.dreamnest.dto.request.UpdateOrderStatusRequest;
import com.dreamnest.dto.request.VerifyPaymentRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.OrderResponse;
import com.dreamnest.dto.response.PageResponse;
import com.dreamnest.dto.response.RazorpayOrderResponse;
import com.dreamnest.dto.response.UpiPaymentInfoResponse;
import com.dreamnest.service.OrderService;
import com.dreamnest.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for placing and managing orders.
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ---- Customer ----

    @PostMapping("/orders/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(@Valid @RequestBody CheckoutRequest request,
                                                                 jakarta.servlet.http.HttpServletRequest httpRequest) {
        OrderResponse response = orderService.checkout(SecurityUtil.getCurrentUserId(), request, extractClientIp(httpRequest));
        return new ResponseEntity<>(ApiResponse.success("Order placed successfully", response), HttpStatus.CREATED);
    }

    /** Reads the real client IP, preferring X-Forwarded-For (set by Nginx/load balancers) over the raw socket address. */
    private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderService.getOrdersForUser(SecurityUtil.getCurrentUserId(), page, size));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long id) {
        boolean privileged = isPrivileged();
        return ApiResponse.success(orderService.getOrderById(SecurityUtil.getCurrentUserId(), id, privileged));
    }

    @GetMapping("/orders/{id}/invoice")
    public org.springframework.http.ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        byte[] pdf = orderService.generateInvoicePdf(SecurityUtil.getCurrentUserId(), id, isPrivileged());
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"Invoice-" + id + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable Long id, @Valid @RequestBody OrderReasonRequest request) {
        return ApiResponse.success("Order cancelled", orderService.cancelOrder(SecurityUtil.getCurrentUserId(), id, request.getReason()));
    }

    @PostMapping("/orders/{id}/return-request")
    public ApiResponse<OrderResponse> requestReturn(@PathVariable Long id, @Valid @RequestBody OrderReasonRequest request) {
        return ApiResponse.success("Return requested", orderService.requestReturn(SecurityUtil.getCurrentUserId(), id, request.getReason()));
    }

    @PutMapping("/business/orders/{id}/return-decision")
    public ApiResponse<OrderResponse> decideReturnAsBusiness(@PathVariable Long id, @RequestParam boolean approve) {
        return ApiResponse.success(orderService.decideReturn(id, SecurityUtil.getCurrentUserId(), false, approve));
    }

    @PutMapping("/admin/orders/{id}/return-decision")
    public ApiResponse<OrderResponse> decideReturnAsAdmin(@PathVariable Long id, @RequestParam boolean approve) {
        return ApiResponse.success(orderService.decideReturn(id, null, true, approve));
    }

    @GetMapping("/orders/{id}/upi-payment")
    public ApiResponse<UpiPaymentInfoResponse> getUpiPaymentInfo(@PathVariable Long id) {
        return ApiResponse.success(orderService.getUpiPaymentInfo(SecurityUtil.getCurrentUserId(), id));
    }

    @PostMapping("/orders/{id}/confirm-payment")
    public ApiResponse<OrderResponse> confirmUpiPayment(@PathVariable Long id) {
        return ApiResponse.success("Payment confirmed", orderService.confirmUpiPayment(SecurityUtil.getCurrentUserId(), id));
    }

    @PostMapping("/orders/{id}/razorpay-order")
    public ApiResponse<RazorpayOrderResponse> createRazorpayOrder(@PathVariable Long id) {
        return ApiResponse.success(orderService.createRazorpayOrder(SecurityUtil.getCurrentUserId(), id));
    }

    @PostMapping("/orders/{id}/verify-payment")
    public ApiResponse<OrderResponse> verifyPayment(@PathVariable Long id, @Valid @RequestBody VerifyPaymentRequest request) {
        return ApiResponse.success("Payment verified", orderService.verifyRazorpayPayment(SecurityUtil.getCurrentUserId(), id, request));
    }

    // ---- Business admin ----

    @GetMapping("/business/orders")
    public ApiResponse<PageResponse<OrderResponse>> getBusinessOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderService.getOrdersForBusiness(SecurityUtil.getCurrentUserId(), page, size));
    }

    @PutMapping("/business/orders/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatusAsBusiness(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.success("Order status updated successfully",
                orderService.updateOrderStatus(id, request, SecurityUtil.getCurrentUserId(), false));
    }

    // ---- Super admin ----

    @GetMapping("/admin/orders")
    public ApiResponse<PageResponse<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderService.getAllOrders(page, size));
    }

    @PutMapping("/admin/orders/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatusAsAdmin(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.success("Order status updated successfully",
                orderService.updateOrderStatus(id, request, null, true));
    }

    private boolean isPrivileged() {
        var principal = SecurityUtil.getCurrentUser();
        return principal != null && ("SUPER_ADMIN".equals(principal.getRole()) || "BUSINESS_ADMIN".equals(principal.getRole()));
    }
}
