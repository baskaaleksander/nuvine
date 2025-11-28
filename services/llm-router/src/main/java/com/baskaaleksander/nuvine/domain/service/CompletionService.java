package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CompletionResponse;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatRequest;
import com.baskaaleksander.nuvine.infrastructure.ai.client.OpenRouterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompletionService {

    private final OpenRouterClient client;

    public CompletionResponse call(String model, String prompt, List<OpenRouterChatRequest.Message> messages) {
        log.info("COMPLETION_CALL START");
        log.info("messages={}", messages);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(new OpenRouterChatRequest.Message("user", prompt));
        var request = new OpenRouterChatRequest(
                model,
                messages,
                0.7,
                2048,
                false
        );

        var response = client.createChatCompletion(request);

        log.info("COMPLETION_CALL END model={} usage={}", response.model(), response.usage());

        return new CompletionResponse(
                response.choices().getFirst().message().content(),
                response.usage().promptTokens(),
                response.usage().completionTokens(),
                response.model()
        );
    }
}
