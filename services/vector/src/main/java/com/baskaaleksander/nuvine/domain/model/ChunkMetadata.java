package com.baskaaleksander.nuvine.domain.model;

import java.util.UUID;

public record ChunkMetadata(
        UUID workspaceId,
        UUID projectId
) {
}
