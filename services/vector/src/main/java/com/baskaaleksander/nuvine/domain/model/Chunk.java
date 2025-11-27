package com.baskaaleksander.nuvine.domain.model;

import java.util.UUID;

public record Chunk(UUID documentId, int page, int startOffset, int endOffset, String content) {
}
