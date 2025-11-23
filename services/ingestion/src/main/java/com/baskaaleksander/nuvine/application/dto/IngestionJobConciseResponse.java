package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;

import java.time.Instant;
import java.util.UUID;

public record IngestionJobConciseResponse(
        UUID documentId,
        IngestionStatus status,
        IngestionStage stage,
        String lastError,
        Instant updatedAt
) {
}
