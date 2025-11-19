package com.baskaaleksander.nuvine.application.dto;

public record UploadUrlRequest(
        String documentId,
        String contentType,
        Long sizeBytes
) {
}
