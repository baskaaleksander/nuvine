package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

import java.time.Instant;

public record LogTokenUsageEvent(
        String workspaceId,
        String userId,
        String conversationId,
        String messageId,
        String model,
        String provider,
        String sourceService,
        long tokensIn,
        long tokensOut,
        Instant occurredAt
) {
}
