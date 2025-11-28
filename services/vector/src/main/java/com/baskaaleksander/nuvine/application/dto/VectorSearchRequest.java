package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        @Size(min = 8, max = 14, message = "Top K must be between 8 and 14")
        int topK,
        @NotNull(message = "Threshold cannot be null")
        @Size(min = 0, max = 1, message = "Threshold must be between 0 and 1")
        long threshold
) {
}
