package com.dreamnest.enums;

/** Lifecycle statuses for an order. */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    RETURN_REQUESTED,
    RETURNED,
    CANCELLED
}
