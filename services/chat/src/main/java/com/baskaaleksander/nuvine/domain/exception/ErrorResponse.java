package com.baskaaleksander.nuvine.domain.exception;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String message,
        Object details,
        String path,
        Instant timestamp
) {
}
