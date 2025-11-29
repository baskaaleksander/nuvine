package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompletionRequest(
        @NotNull
        String message,
        @NotNull
        String model,
        List<OpenRouterChatStreamRequest.Message> messages
) {
}
