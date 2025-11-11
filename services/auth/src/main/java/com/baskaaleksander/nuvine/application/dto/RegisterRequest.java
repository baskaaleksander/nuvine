package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull(message = "First name must not be null")
        String firstName,
        @NotNull(message = "Last name must not be null")
        String lastName,
        @NotNull(message = "Email must not be null")
        @Email(message = "Email should be valid")
        String email,
        @NotNull(message = "Password must not be null")
        String password
) {
}
