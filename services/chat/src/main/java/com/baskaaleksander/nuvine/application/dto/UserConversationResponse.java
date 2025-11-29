package com.baskaaleksander.nuvine.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserConversationResponse(
        UUID conversationId,
        String lastMessage,
        Instant lastMessageAt
) {
}
