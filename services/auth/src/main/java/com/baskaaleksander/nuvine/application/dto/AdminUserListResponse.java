package com.baskaaleksander.nuvine.application.dto;

import java.util.UUID;

public record AdminUserListResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        boolean emailVerified,
        boolean onboardingCompleted
) {
}
