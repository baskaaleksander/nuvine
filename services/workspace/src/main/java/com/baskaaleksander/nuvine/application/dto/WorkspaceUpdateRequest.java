package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record WorkspaceUpdateRequest(
        @NotNull(message = "Name cannot be null")
        String name
) {
}
