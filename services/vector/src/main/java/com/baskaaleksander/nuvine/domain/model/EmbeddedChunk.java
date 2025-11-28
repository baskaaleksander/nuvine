package com.baskaaleksander.nuvine.domain.model;

import java.util.List;
import java.util.UUID;

public record EmbeddedChunk(UUID documentId, int page, int startOffset, int endOffset, List<Float> embedding,
                            String content,
                            int index) {
}
