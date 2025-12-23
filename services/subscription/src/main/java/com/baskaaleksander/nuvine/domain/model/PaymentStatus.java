package com.baskaaleksander.nuvine.domain.model;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    REFUNDED,
    CANCELED,
    REQUIRES_ACTION
}
