package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record PasswordResetEvent(
        String email,
        String token,
        String userId
) {
}

