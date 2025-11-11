package com.baskaaleksander.nuvine.application.dto;

import java.util.List;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        List<String> roles,
        boolean emailVerified,
        boolean onboardingCompleted
) {
}
