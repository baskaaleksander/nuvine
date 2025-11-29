package com.baskaaleksander.nuvine.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenRouterChatStreamRequest(
        String model,
        List<Message> messages,
        Double temperature,
        @JsonProperty("max_tokens")
        Integer maxTokens,
        boolean stream,
        @JsonProperty("stream_options")
        StreamOptions streamOptions
) {
    public record Message(String role, String content) {
    }

    public record StreamOptions(
            @JsonProperty("include_usage")
            boolean includeUsage
    ) {
    }
}
