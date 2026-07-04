package com.dreamnest.enums;

/** Lifecycle of a business payout, from when the platform collects the customer's payment through final settlement. */
public enum PayoutStatus {
    PENDING,
    PROCESSING,
    PAID,
    PARTIALLY_PAID,
    FAILED,
    CANCELLED,
    REVERSED,
    DISPUTED
}
