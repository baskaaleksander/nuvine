package com.baskaaleksander.nuvine.application.dto;

import java.util.UUID;

public record CustomerPortalSessionRequest(
        UUID workspaceId
) {
}
