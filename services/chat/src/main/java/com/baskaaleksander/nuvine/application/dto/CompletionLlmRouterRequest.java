package com.baskaaleksander.nuvine.application.dto;

import java.util.List;

public record CompletionLlmRouterRequest(
        String message,
        String model,
        List<Message> messages
) {
    public record Message(String role, String content) {
    }
}
