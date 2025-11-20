package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record UploadCompletedRequest(
        @NotNull(message = "Storage key is required")
        String storageKey,
        @NotNull(message = "Mime type is required")
        String mimeType,
        @NotNull(message = "Size bytes is required")
        Long sizeBytes
) {
}
