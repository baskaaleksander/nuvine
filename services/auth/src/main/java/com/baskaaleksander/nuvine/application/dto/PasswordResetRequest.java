package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record PasswordResetRequest(
        @NotNull(message = "Password reset token cannot be null")
        String token,
        @NotNull(message = "Password cannot be null")
        String password,
        @NotNull(message = "Confirm password cannot be null")
        String confirmPassword
) {
}
