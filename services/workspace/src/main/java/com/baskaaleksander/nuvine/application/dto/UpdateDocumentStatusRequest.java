package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDocumentStatusRequest(
        @NotNull(message = "Status is required")
        DocumentStatus status
) {
}
