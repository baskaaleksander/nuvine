package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID id,
        UUID workspaceId,
        UUID userId,
        WorkspaceRole role,
        Instant createdAt
) {
}
