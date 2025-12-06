package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.BillingTier;

import java.util.UUID;

public record WorkspaceInternalSubscriptionResponse(
        UUID id,
        String name,
        BillingTier billingTier,
        String stripeSubscriptionId,
        UUID ownerId
) {
}
