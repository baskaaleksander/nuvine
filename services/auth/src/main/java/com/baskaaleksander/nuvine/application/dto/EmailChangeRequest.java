package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record EmailChangeRequest(
        @NotNull(message = "Password cannot be null")
        String password,
        @NotNull(message = "Email cannot be null")
        @Email(message = "Email is not valid")
        String email
) {
}
