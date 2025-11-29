package com.baskaaleksander.nuvine.application.dto;

public record LlmChunk(
        String type,
        String content,
        Integer tokensIn,
        Integer tokensOut
) {
}
