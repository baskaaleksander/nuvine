package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.util.List;

public record EmbeddingRequestEvent(
        String embeddingJobId,
        List<String> texts,
        List<Integer> chunkIndexes,
        String model
) {
}
