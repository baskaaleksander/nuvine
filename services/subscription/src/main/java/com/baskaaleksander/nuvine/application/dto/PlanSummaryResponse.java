package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.BillingPeriod;

import java.util.UUID;

public record PlanSummaryResponse(
        UUID id,
        String code,
        String name,
        BillingPeriod billingPeriod,
        long includedCredits
) {
}
