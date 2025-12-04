package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.PaymentSessionIntent;

import java.util.UUID;

public record PaymentSessionRequest(
        UUID workspaceId,
        UUID planId,
        PaymentSessionIntent intent
) {
}
