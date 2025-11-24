package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;

import java.time.Instant;
import java.util.UUID;

public record IngestionJobResponse(
        UUID id,
        UUID documentId,
        UUID workspaceId,
        UUID projectId,
        IngestionStatus status,
        IngestionStage stage,
        int retryCount,
        String lastError,
        Instant updatedAt,
        Instant createdAt
) {
}
