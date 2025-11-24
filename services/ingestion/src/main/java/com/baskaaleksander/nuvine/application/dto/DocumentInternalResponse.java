package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentInternalResponse(
        UUID id,
        UUID projectId,
        UUID workspaceId,
        String name,
        DocumentStatus status,
        String storageKey,
        String mimeType,
        Long sizeBytes,
        UUID createdBy,
        Instant createdAt

) {
}
