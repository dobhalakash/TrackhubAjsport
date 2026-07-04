package com.dreamnest.enums;

/** Lifecycle of a shipment, from creation through delivery (or return). */
public enum ShipmentStatus {
    PENDING,
    SHIPMENT_CREATED,
    AWB_ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    RTO,
    CANCELLED
}
