package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record TextVectorSearchRequest(
        @NotNull(message = "Workspace Id cannot be null")
        UUID workspaceId,
        @NotNull(message = "Project Id cannot be null")
        UUID projectId,
        @NotEmpty(message = "Document Ids cannot be empty")
        List<UUID> documentIds,
        @NotNull(message = "Query cannot be null")
        String query,
        @NotNull(message = "Top K cannot be null")
        int topK,
        float threshold
) {
}
