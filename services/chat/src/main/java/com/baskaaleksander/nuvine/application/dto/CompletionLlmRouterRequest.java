package com.baskaaleksander.nuvine.application.dto;

public record CompletionLlmRouterRequest(
        String message,
        String model
) {
}
