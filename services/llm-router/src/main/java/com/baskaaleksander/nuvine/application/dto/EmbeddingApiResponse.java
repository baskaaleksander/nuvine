package com.baskaaleksander.nuvine.application.dto;

import java.util.List;

public record EmbeddingApiResponse(
        String model,
        List<EmbeddingData> data
) {
    public record EmbeddingData(
            int index,
            List<Float> embedding
    ) {
    }
}
