package com.dreamnest.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Used by a business admin to manually record tracking details for an
 * order, e.g. when shipping directly through a courier rather than via the
 * Shiprocket integration.
 */
public class ManualShipmentRequest {

    @NotBlank(message = "Courier name is required")
    private String courierName;

    @NotBlank(message = "AWB / tracking number is required")
    private String awbNumber;

    private String trackingUrl;

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getAwbNumber() {
        return awbNumber;
    }

    public void setAwbNumber(String awbNumber) {
        this.awbNumber = awbNumber;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }
}
