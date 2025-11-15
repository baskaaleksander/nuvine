package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record PasswordChangeRequest(
        @NotNull(message = "Old password cannot be null")
        String oldPassword,
        @NotNull(message = "New password cannot be null")
        String newPassword,
        @NotNull(message = "Confirm new password cannot be null")
        String confirmNewPassword
) {
}
