package com.baskaaleksander.nuvine.application.dto;

import java.util.List;
import java.util.UUID;

public record CompletionRequest(
        UUID workspaceId,
        UUID projectId,
        List<UUID> documentIds,
        UUID conversationId,
        String message,
        String model,
        int memorySize,
        boolean strictMode,
        boolean freeMode
) {
}
