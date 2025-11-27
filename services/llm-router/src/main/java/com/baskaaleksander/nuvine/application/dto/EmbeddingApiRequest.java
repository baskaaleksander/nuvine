package com.baskaaleksander.nuvine.application.dto;

import java.util.List;

public record EmbeddingApiRequest(
        String model,
        List<String> input
) {
}
