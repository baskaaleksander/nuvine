package com.baskaaleksander.subscription.domain.model;

public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    VOID
}
