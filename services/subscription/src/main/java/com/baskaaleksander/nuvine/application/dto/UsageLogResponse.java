package com.baskaaleksander.nuvine.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UsageLogResponse(
        UUID id,
        UUID userId,
        UUID conversationId,
        UUID messageId,
        String provider,
        String model,
        long tokensIn,
        long tokensOut,
        BigDecimal costCredits,
        Instant occurredAt
) {
}
