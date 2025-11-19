package com.baskaaleksander.nuvine.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID projectId,
        UUID workspaceId,
        String name,
        String status,
        UUID createdBy,
        Instant createdAt
) {
}
