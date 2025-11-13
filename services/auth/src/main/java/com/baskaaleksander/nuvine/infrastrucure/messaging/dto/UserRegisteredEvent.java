package com.baskaaleksander.nuvine.infrastrucure.messaging.dto;

public record UserRegisteredEvent(
        String firstName,
        String lastName,
        String email,
        String emailVerificationToken
) {
}
