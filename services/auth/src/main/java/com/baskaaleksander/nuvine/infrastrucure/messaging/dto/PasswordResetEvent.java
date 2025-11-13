package com.baskaaleksander.nuvine.infrastrucure.messaging.dto;

public record PasswordResetEvent(
        String email,
        String token
) {
}
