package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import com.baskaaleksander.nuvine.domain.model.Chunk;

import java.util.List;

public record EmbeddingRequestEvent(
        String embeddingJobId,
        List<Chunk> chunks,
        String model
) {
}
