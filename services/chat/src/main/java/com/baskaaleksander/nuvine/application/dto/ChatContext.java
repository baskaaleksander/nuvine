package com.baskaaleksander.nuvine.application.dto;


import java.util.List;
import java.util.UUID;

public record ChatContext(
        String prompt,
        UUID conversationId,
        UUID projectId,
        UUID workspaceId,
        List<CompletionLlmRouterRequest.Message> messages,
        UUID ownerId
) {
}
