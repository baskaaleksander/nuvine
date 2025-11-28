package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record VectorSearchRequest(
        @NotNull(message = "Workspace Id cannot be null")
        UUID workspaceId,
        @NotNull(message = "Project Id cannot be null")
        UUID projectId,
        @NotEmpty(message = "Document Ids cannot be empty")
        List<UUID> documentIds,
        @NotEmpty(message = "Query cannot be empty")
        List<Float> query,
        @NotNull(message = "Top K cannot be null")
        int topK,
        @NotNull(message = "Threshold cannot be null")
        float threshold
) {
}
