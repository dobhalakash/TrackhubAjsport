package com.dreamnest.service;

import com.dreamnest.dto.request.ManualShipmentRequest;
import com.dreamnest.dto.response.ShipmentResponse;

/**
 * Manages shipment/tracking creation and updates for orders, once payment
 * has been confirmed.
 */
public interface ShipmentService {

    /** Creates a shipment + requests an AWB automatically via the Shiprocket API. */
    ShipmentResponse createViaShiprocket(Long orderId, Long businessUserId, boolean isAdmin);

    /** Records (or updates) tracking details entered manually by the business admin. */
    ShipmentResponse saveManual(Long orderId, Long businessUserId, boolean isAdmin, ManualShipmentRequest request);

    /** Re-fetches the latest status from Shiprocket for a shipment that was created via the API. */
    ShipmentResponse refreshTracking(Long orderId, Long businessUserId, boolean isAdmin);

    /** Looks up an order's shipment for the support/tracking lookup tools. Returns null if none exists yet. */
    ShipmentResponse getByOrderId(Long orderId);

    /** Applies an inbound Shiprocket tracking webhook update, matched by AWB number. Silently ignores unknown AWBs. */
    void applyWebhookUpdate(String awbNumber, String status, String note);
}
