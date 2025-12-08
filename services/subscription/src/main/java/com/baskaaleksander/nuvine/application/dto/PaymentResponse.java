package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        String currency,
        PaymentStatus status,
        Instant billingPeriodStart,
        Instant billingPeriodEnd,
        String invoicePdfUrl,
        String description,
        Instant createdAt
) {
}
