package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record UserRegisteredEvent(
        String firstName,
        String lastName,
        String email,
        String emailVerificationToken
) {
}
