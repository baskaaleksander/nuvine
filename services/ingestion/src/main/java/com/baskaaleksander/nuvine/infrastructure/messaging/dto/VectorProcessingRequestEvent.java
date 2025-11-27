package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import com.baskaaleksander.nuvine.domain.model.Chunk;

import java.util.List;

public record VectorProcessingRequestEvent(
        String ingestionJobId,
        String documentId,
        String projectId,
        String workspaceId,
        List<Chunk> chunks
) {
}
