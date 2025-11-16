package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record WorkspaceMemberRoleRequest(
        @NotNull(message = "Role cannot be null")
        WorkspaceRole role
) {
}
