package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull(message = "Email must not be null")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Password must not be null")
        String password
) {
}
