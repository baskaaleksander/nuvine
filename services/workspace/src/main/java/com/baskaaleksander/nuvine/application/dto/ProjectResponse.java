package com.baskaaleksander.nuvine.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        String name,
        String description,
        UUID workspaceId,
        Instant createdAt
) {
}
