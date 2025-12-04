package com.baskaaleksander.subscription.application.dto;

import com.baskaaleksander.subscription.domain.model.PaymentSessionIntent;

import java.util.UUID;

public record PaymentSessionRequest(
        UUID workspaceId,
        UUID planId,
        PaymentSessionIntent intent,
        UUID userId,
        String email
) {
}
