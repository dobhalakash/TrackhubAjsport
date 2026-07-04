package com.dreamnest.controller;

import com.dreamnest.dto.request.ManualShipmentRequest;
import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.dto.response.OrderResponse;
import com.dreamnest.dto.response.ShipmentResponse;
import com.dreamnest.util.SecurityUtil;
import com.dreamnest.service.OrderService;
import com.dreamnest.service.ShipmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Shipping/tracking management for business admins and platform admins,
 * plus a lookup tool for support staff to find an order's current shipment
 * status by order number, customer email, or AWB number.
 */
@RestController
public class ShipmentController {

    private static final Logger log = LoggerFactory.getLogger(ShipmentController.class);

    private final ShipmentService shipmentService;
    private final OrderService orderService;

    @Value("${dreamnest.shiprocket.webhook-token}")
    private String webhookToken;

    public ShipmentController(ShipmentService shipmentService, OrderService orderService) {
        this.shipmentService = shipmentService;
        this.orderService = orderService;
    }

    // ── Business admin ──────────────────────────────────────────────────

    @PostMapping("/business/orders/{id}/shipment/shiprocket")
    public ApiResponse<ShipmentResponse> createShiprocketShipmentAsBusiness(@PathVariable Long id) {
        return ApiResponse.success("Shipment created and AWB assigned",
                shipmentService.createViaShiprocket(id, SecurityUtil.getCurrentUserId(), false));
    }

    @PostMapping("/business/orders/{id}/shipment/manual")
    public ApiResponse<ShipmentResponse> saveManualShipmentAsBusiness(@PathVariable Long id,
                                                                       @Valid @RequestBody ManualShipmentRequest request) {
        return ApiResponse.success("Tracking details saved",
                shipmentService.saveManual(id, SecurityUtil.getCurrentUserId(), false, request));
    }

    @PostMapping("/business/orders/{id}/shipment/refresh")
    public ApiResponse<ShipmentResponse> refreshShipmentAsBusiness(@PathVariable Long id) {
        return ApiResponse.success(shipmentService.refreshTracking(id, SecurityUtil.getCurrentUserId(), false));
    }

    // ── Platform admin ───────────────────────────────────────────────────

    @PostMapping("/admin/orders/{id}/shipment/shiprocket")
    public ApiResponse<ShipmentResponse> createShiprocketShipmentAsAdmin(@PathVariable Long id) {
        return ApiResponse.success("Shipment created and AWB assigned",
                shipmentService.createViaShiprocket(id, null, true));
    }

    @PostMapping("/admin/orders/{id}/shipment/manual")
    public ApiResponse<ShipmentResponse> saveManualShipmentAsAdmin(@PathVariable Long id,
                                                                    @Valid @RequestBody ManualShipmentRequest request) {
        return ApiResponse.success("Tracking details saved",
                shipmentService.saveManual(id, null, true, request));
    }

    @PostMapping("/admin/orders/{id}/shipment/refresh")
    public ApiResponse<ShipmentResponse> refreshShipmentAsAdmin(@PathVariable Long id) {
        return ApiResponse.success(shipmentService.refreshTracking(id, null, true));
    }

    /**
     * Support lookup tool: search by order number, customer email, or AWB
     * number to see exactly where a customer's package is - same
     * information a business admin sees, surfaced for support staff
     * without requiring them to know which business owns the order.
     */
    @GetMapping("/admin/support/orders")
    public ApiResponse<List<OrderResponse>> searchForSupport(@RequestParam String q) {
        return ApiResponse.success(orderService.searchForSupport(q));
    }

    /**
     * Inbound Shiprocket tracking webhook. Configure this URL in Shiprocket
     * (Settings -> API -> Webhooks) with a "token" query param matching
     * SHIPROCKET_WEBHOOK_TOKEN, e.g.:
     * https://yourdomain.com/api/shipments/shiprocket/webhook?token=...
     *
     * Shiprocket does not cryptographically sign webhook payloads the way
     * Razorpay does, so this shared-secret query param is the best
     * available defense against spoofed requests.
     */
    @PostMapping("/shipments/shiprocket/webhook")
    public ApiResponse<String> shiprocketWebhook(@RequestParam(required = false) String token,
                                                  @RequestBody Map<String, Object> payload) {
        if (webhookToken == null || webhookToken.startsWith("dummy") || !webhookToken.equals(token)) {
            log.warn("Rejected Shiprocket webhook with invalid/missing token");
            return ApiResponse.success("ignored", "Invalid token");
        }

        try {
            String awb = stringValue(payload.get("awb"));
            String status = stringValue(payload.get("current_status"));
            String note = stringValue(payload.get("current_status_body"));
            shipmentService.applyWebhookUpdate(awb, status, note);
            return ApiResponse.success("ok", "Webhook processed");
        } catch (Exception e) {
            log.error("Failed to process Shiprocket webhook", e);
            return ApiResponse.success("error", "Could not process webhook");
        }
    }

    private String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
