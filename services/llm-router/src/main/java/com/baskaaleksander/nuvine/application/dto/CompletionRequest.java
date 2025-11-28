package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record CompletionRequest(
        @NotNull
        String message,
        @NotNull
        String model
) {
}
