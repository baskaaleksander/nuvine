package com.baskaaleksander.nuvine.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectDetailedResponse(
        String name,
        String description,
        UUID workspaceId,
        Long documentCount,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}
