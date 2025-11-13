package com.baskaaleksander.nuvine.infrastrucure.messaging.dto;

public record EmailVerificationEvent(
        String email,
        String token
) {
}
