package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.BillingTier;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        UUID ownerUserId,
        String subscriptionId,
        BillingTier billingTier,
        Instant createdAt,
        Instant updatedAt
) {
}
