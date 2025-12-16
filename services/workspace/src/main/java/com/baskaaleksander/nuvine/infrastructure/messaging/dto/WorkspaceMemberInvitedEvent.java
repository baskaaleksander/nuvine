package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record WorkspaceMemberInvitedEvent(
        String email,
        String workspaceId,
        String workspaceName,
        String role
) {
}
