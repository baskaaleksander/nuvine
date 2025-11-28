package com.baskaaleksander.nuvine.application.dto;


import java.util.List;

public record OpenRouterChatResponse(
        String id,
        String object,
        Long created,
        String model,
        List<Choice> choices
) {
    public record Choice(
            int index,
            Message message,
            String finish_reason
    ) {
        public record Message(String role, String content) {
        }
    }
}
