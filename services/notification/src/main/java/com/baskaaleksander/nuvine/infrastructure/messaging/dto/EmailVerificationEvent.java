package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.util.UUID;

public record EmailVerificationEvent(
        String email,
        String token,
        String userId
) {
}
