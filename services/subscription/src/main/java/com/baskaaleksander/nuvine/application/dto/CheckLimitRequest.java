package com.baskaaleksander.nuvine.application.dto;

import java.util.UUID;

public record CheckLimitRequest(
        UUID workspaceId,
        String modelKey,
        String providerKey,
        long inputTokens
) {
}
