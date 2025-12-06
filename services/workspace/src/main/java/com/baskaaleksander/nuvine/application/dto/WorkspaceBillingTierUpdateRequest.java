package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record WorkspaceBillingTierUpdateRequest(
        @NotNull(message = "Billing tier code cannot be null")
        String billingTierCode
) {
}
