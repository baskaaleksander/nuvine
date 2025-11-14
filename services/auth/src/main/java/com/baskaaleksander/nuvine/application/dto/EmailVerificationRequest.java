package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record EmailVerificationRequest(
        @NotNull(message = "Token must not be null")
        String token
) {
}
