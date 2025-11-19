package com.baskaaleksander.nuvine.application.dto;

import java.util.UUID;

public record ParsedStorageKey(
        UUID workspaceId,
        UUID projectId,
        UUID documentId
) {
}
