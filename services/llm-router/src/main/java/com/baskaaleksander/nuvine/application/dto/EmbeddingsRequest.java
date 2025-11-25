package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record EmbeddingsRequest(
        @NotNull
        String input
) {
}
