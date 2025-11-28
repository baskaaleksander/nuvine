package com.baskaaleksander.nuvine.application.dto;

import java.util.List;
import java.util.UUID;

public record VectorSearchResponse(
        List<VectorSearchMatch> matches
) {

    public record VectorSearchMatch(
            UUID documentId,
            int page,
            int startOffset,
            int endOffset,
            String content,
            float score
    ) {
    }
}
