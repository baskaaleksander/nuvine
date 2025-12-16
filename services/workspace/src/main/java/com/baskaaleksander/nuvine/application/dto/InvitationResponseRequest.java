package com.baskaaleksander.nuvine.application.dto;

import jakarta.validation.constraints.NotNull;

public record InvitationResponseRequest(
        @NotNull(message = "Action is required")
        InvitationAction action
) {
}
