package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record ForgotPasswordRequest(
        @NotNull(message = "Email cannot be null")
        @Email(message = "Email must be valid")
        String email
) {
}
