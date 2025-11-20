package com.baskaaleksander.nuvine.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentInternalResponse(
        UUID id,
        UUID projectId,
        UUID workspaceId,
        String name,
        String status,
        String storageKey,
        String mimeType,
        Long sizeBytes,
        UUID createdBy,
        Instant createdAt

) {
}
