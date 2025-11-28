package com.baskaaleksander.nuvine.application.dto;

public record CompletionResponse(
        String content,
        int tokensIn,
        int tokensOut,
        String modelUsed
) {
}
