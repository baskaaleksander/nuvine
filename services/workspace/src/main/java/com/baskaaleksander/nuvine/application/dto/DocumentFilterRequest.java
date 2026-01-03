package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.DocumentStatus;

import java.time.Instant;

public record DocumentFilterRequest(
        DocumentStatus status,
        Instant createdAtFrom,
        Instant createdAtTo
) {
}
