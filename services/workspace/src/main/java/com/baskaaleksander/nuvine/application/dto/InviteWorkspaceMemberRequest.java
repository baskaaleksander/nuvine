package com.baskaaleksander.nuvine.application.dto;

import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteWorkspaceMemberRequest(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must be a valid email address")
        String email,
        @NotNull(message = "Role cannot be null")
        WorkspaceRole role
) {
}
