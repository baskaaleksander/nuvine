package com.baskaaleksander.nuvine.application.dto;

import java.util.List;
import java.util.UUID;

public record EmbeddingRequest(
        List<String> texts,
        String model,
        UUID workspaceId,
        UUID projectId
) {
}
