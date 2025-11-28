package com.baskaaleksander.nuvine.application.dto;

import java.util.UUID;

public record CompletionRequest(
        UUID conversationId,
        String message,
        String model,
        int memorySize
) {
}
