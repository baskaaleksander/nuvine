package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WorkspaceMemberRequest(
        @NotNull(message = "User Id cannot be null")
        UUID userId,
        @NotNull(message = "Role cannot be null")
        WorkspaceRole role
) {
}
