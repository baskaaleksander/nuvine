package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CompletionRequest(
        @NotNull
        UUID workspaceId,
        @NotNull
        UUID projectId,
        List<UUID> documentIds,
        UUID conversationId,
        @NotNull
        String message,
        @NotNull
        String model,
        @NotNull
        int memorySize,
        boolean strictMode,
        boolean freeMode
) {
}
