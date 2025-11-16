package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 255, message = "Name cannot be longer than 255 characters")
        String name,
        @Size(max = 255, message = "Description cannot be longer than 255 characters")
        String description
) {
}
