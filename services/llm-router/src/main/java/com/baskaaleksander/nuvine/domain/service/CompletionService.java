package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CompletionResponse;
import com.baskaaleksander.nuvine.application.dto.LlmChunk;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatRequest;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatStreamRequest;
import com.baskaaleksander.nuvine.infrastructure.ai.client.OpenRouterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompletionService {

    private final OpenRouterClient client;
    private final OpenRouterStreamService openRouterStreamService;

    public CompletionResponse call(String model, String prompt, List<OpenRouterChatStreamRequest.Message> messages) {
        log.info("COMPLETION_CALL START");
        log.info("messages={}", messages);

        List<OpenRouterChatStreamRequest.Message> msgs =
                (messages == null) ? new ArrayList<>() : new ArrayList<>(messages);

        msgs.add(new OpenRouterChatStreamRequest.Message("user", prompt));

        var response = client.createChatCompletion(new OpenRouterChatRequest(
                model,
                msgs,
                0.7,
                2048,
                false
        ));

        log.info("COMPLETION_CALL END model={} usage={}", response.model(), response.usage());

        return new CompletionResponse(
                response.choices().getFirst().message().content(),
                response.usage().promptTokens(),
                response.usage().completionTokens(),
                response.model()
        );
    }

    public Flux<LlmChunk> callStream(String model, String prompt, List<OpenRouterChatStreamRequest.Message> messages) {
        log.info("COMPLETION_CALL_STREAM START");

        OpenRouterChatStreamRequest request = buildStreamRequest(model, prompt, messages);

        return openRouterStreamService.stream(request)
                .doOnComplete(() -> log.info("COMPLETION_CALL_STREAM END"));
    }

    private OpenRouterChatStreamRequest buildStreamRequest(
            String model,
            String prompt,
            List<OpenRouterChatStreamRequest.Message> messages
    ) {
        List<OpenRouterChatStreamRequest.Message> msgs =
                (messages == null) ? new ArrayList<>() : new ArrayList<>(messages);

        msgs.add(new OpenRouterChatStreamRequest.Message("user", prompt));

        return new OpenRouterChatStreamRequest(
                model,
                msgs,
                0.7,
                2048,
                true,
                new OpenRouterChatStreamRequest.StreamOptions(true)
        );
    }
}