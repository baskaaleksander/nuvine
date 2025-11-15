package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record WorkspaceCreateRequest(
        @NotNull(message = "Name cannot be null")
        String name
) {
}
