package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID projectId,
        UUID workspaceId,
        String name,
        DocumentStatus status,
        UUID createdBy,
        Instant createdAt
) {
}
