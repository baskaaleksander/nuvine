package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record DocumentUploadedEvent(
        String documentId,
        String workspaceId,
        String projectId,
        String storageKey,
        String mimeType,
        long sizeBytes
) {
}
