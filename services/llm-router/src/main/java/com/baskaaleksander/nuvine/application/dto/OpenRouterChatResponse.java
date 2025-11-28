package com.baskaaleksander.nuvine.application.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenRouterChatResponse(
        String id,
        String object,
        Long created,
        String model,
        List<Choice> choices,
        Usage usage
) {
    public record Choice(
            int index,
            Message message,
            String finish_reason
    ) {
        public record Message(String role, String content) {
        }
    }

    public record Usage(
            @JsonProperty("completion_tokens")
            Integer completionTokens,
            @JsonProperty("prompt_tokens")
            Integer promptTokens,
            @JsonProperty("total_tokens")
            Integer totalTokens,
            @JsonProperty("completion_tokens_details")
            CompletionTokensDetails completionTokensDetails,
            @JsonProperty("prompt_tokens_details")
            PromptTokensDetails promptTokensDetails
    ) {
        public record CompletionTokensDetails(
                @JsonProperty("reasoning_tokens")
                Integer reasoningTokens,
                @JsonProperty("audio_tokens")
                Integer audioTokens,
                @JsonProperty("accepted_prediction_tokens")
                Integer acceptedPredictionTokens,
                @JsonProperty("rejected_prediction_tokens")
                Integer rejectedPredictionTokens
        ) {
        }

        public record PromptTokensDetails(
                @JsonProperty("cached_tokens")
                Integer cachedTokens,
                @JsonProperty("audio_tokens")
                Integer audioTokens,
                @JsonProperty("video_tokens")
                Integer videoTokens
        ) {
        }
    }
}
