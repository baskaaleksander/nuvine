package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;

import java.util.List;

public record EmbeddingCompletedEvent(
        String ingestionJobId,
        List<EmbeddedChunk> embeddedChunks,
        String model
) {
}
