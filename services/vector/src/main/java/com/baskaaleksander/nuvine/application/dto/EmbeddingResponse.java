package com.baskaaleksander.nuvine.application.dto;

import java.util.List;

public record EmbeddingResponse(
        List<List<Float>> embeddings,
        String usedModel
) {
}
