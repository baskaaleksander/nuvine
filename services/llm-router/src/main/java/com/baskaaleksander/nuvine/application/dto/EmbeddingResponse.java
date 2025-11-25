package com.baskaaleksander.nuvine.application.dto;

import java.util.List;

public record EmbeddingResponse(
        List<Embedding> embeddings,
        String usedModel
) {

    public record Embedding(
            int index,
            List<Float> vector
    ) {
    }
}
