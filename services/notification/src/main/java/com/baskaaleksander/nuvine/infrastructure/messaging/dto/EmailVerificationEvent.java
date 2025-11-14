package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record EmailVerificationEvent(
        String email,
        String token,
        String userId
) {
}
