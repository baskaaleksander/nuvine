package com.baskaaleksander.nuvine.infrastructure.messaging.dto;

public record UpdateWorkspaceMemberDataEvent(
        String userId,
        String firstName,
        String lastName,
        String email
) {
}
