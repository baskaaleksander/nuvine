package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkspaceCreateRequest(
        @NotNull(message = "Name cannot be null")
        @Size(max = 255, message = "Name cannot be longer than 255 characters")
        String name
) {
}
