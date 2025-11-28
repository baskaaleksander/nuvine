package com.baskaaleksander.nuvine.application.dto;

import java.util.List;

public record EmbeddingRequest(
        List<String> texts,
        String model
) {
}
