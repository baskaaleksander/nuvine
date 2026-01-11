package com.baskaaleksander.nuvine.application.dto;

public record WorkspaceBillingTierUpdateRequest(
        String billingTierCode,
        String stripeSubscriptionId
) {
}
