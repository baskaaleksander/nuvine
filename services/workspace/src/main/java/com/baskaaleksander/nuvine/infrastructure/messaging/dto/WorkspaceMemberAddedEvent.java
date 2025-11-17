package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record WorkspaceMemberAddedEvent(
        String email,
        String userId,
        String workspaceId,
        String role
) {
}
