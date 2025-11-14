package com.baskaaleksander.nuvine.application.dto;

import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        boolean emailVerified,
        boolean onboardingCompleted,
        List<String> roles
) {
}
