package com.baskaaleksander.nuvine.application.dto;

import java.util.List;

public record OpenRouterChatRequest(
        String model,
        List<OpenRouterChatStreamRequest.Message> messages,
        Double temperature,
        Integer max_tokens,
        Boolean stream
) {
    public record Message(String role, String content) {
    }

}
