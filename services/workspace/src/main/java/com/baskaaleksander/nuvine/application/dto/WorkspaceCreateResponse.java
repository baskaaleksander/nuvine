package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.BillingTier;

import java.util.UUID;

public record WorkspaceCreateResponse(
        UUID id,
        String name,
        UUID ownerUserId,
        BillingTier billingTier
) {
}
