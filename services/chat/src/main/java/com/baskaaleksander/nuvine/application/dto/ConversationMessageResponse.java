package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.ConversationRole;

import java.time.Instant;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID id,
        UUID conversationId,
        String content,
        ConversationRole role,
        String modelUsed,
        int tokensCost,
        UUID ownerId,
        Instant createdAt
) {
}
