package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record VectorProcessingCompletedEvent(
        String ingestionJobId,
        String documentId,
        String projectId,
        String workspaceId
) {
}