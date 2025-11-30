package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record DocumentIngestionCompletedEvent(
        String documentId,
        String workspaceId,
        String projectId
) {
}
