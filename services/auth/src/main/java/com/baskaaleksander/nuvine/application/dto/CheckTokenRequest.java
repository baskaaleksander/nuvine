package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record CheckTokenRequest(
        @NotNull(message = "Token cannot be null")
        String token
) {
}
