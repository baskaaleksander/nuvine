package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.util.UUID;

public record UserRegisteredEvent(
        String firstName,
        String lastName,
        String email,
        String emailVerificationToken,
        String userId
) {
}
