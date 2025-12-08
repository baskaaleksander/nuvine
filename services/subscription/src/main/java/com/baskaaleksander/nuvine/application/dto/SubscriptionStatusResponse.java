package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionStatusResponse(
        UUID id,
        SubscriptionStatus status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        PlanSummaryResponse plan,
        UsageSummaryResponse usage
) {
}
