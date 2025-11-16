package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotNull(message = "Name cannot be null")
        String name,
        @Size(max = 255, message = "Description cannot be longer than 255 characters")
        String description
) {
}
