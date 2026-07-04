package com.dreamnest.mapper;

import com.dreamnest.dto.response.ShipmentResponse;
import com.dreamnest.entity.Shipment;

public class ShipmentMapper {

    private ShipmentMapper() {
    }

    public static ShipmentResponse toResponse(Shipment shipment) {
        if (shipment == null) {
            return null;
        }
        ShipmentResponse response = new ShipmentResponse();
        response.setOrderId(shipment.getOrder().getId());
        response.setOrderNumber(shipment.getOrder().getOrderNumber());
        response.setStatus(shipment.getStatus() != null ? shipment.getStatus().name() : null);
        response.setCourierName(shipment.getCourierName());
        response.setAwbNumber(shipment.getAwbNumber());
        response.setTrackingUrl(shipment.getTrackingUrl());
        response.setSource(shipment.getSource());
        response.setLastTrackingNote(shipment.getLastTrackingNote());
        response.setShippedAt(shipment.getShippedAt());
        response.setDeliveredAt(shipment.getDeliveredAt());
        response.setLastSyncedAt(shipment.getLastSyncedAt());
        return response;
    }
}
