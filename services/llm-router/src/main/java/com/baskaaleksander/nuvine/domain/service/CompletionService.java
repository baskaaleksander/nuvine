package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CompletionResponse;
import com.baskaaleksander.nuvine.application.dto.LlmChunk;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatRequest;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatResponse;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatStreamRequest;
import com.baskaaleksander.nuvine.domain.exception.ModelCircuitBreakerOpenException;
import com.baskaaleksander.nuvine.infrastructure.ai.client.OpenRouterClient;
import com.baskaaleksander.nuvine.infrastructure.resilience.OpenRouterCircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompletionService {

    private final OpenRouterClient client;
    private final OpenRouterStreamService openRouterStreamService;
    private final OpenRouterCircuitBreakerRegistry circuitBreakerRegistry;

    public CompletionResponse call(String model, String prompt, List<OpenRouterChatStreamRequest.Message> messages) {
        log.info("COMPLETION_CALL START model={}", model);
        log.info("messages={}", messages);

        List<OpenRouterChatStreamRequest.Message> msgs =
                (messages == null) ? new ArrayList<>() : new ArrayList<>(messages);

        msgs.add(new OpenRouterChatStreamRequest.Message("user", prompt));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.getCircuitBreaker(model);

        Supplier<OpenRouterChatResponse> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, () -> client.createChatCompletion(new OpenRouterChatRequest(
                        model,
                        msgs,
                        0.7,
                        2048,
                        false
                )));

        OpenRouterChatResponse response;
        try {
            response = decoratedSupplier.get();
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker OPEN for model: {}", model);
            throw new ModelCircuitBreakerOpenException(model, e);
        }

        log.info("COMPLETION_CALL END model={} usage={}", response.model(), response.usage());

        return new CompletionResponse(
                response.choices().getFirst().message().content(),
                response.usage().promptTokens(),
                response.usage().completionTokens(),
                response.model()
        );
    }

    public Flux<LlmChunk> callStream(String model, String prompt, List<OpenRouterChatStreamRequest.Message> messages) {
        log.info("COMPLETION_CALL_STREAM START model={}", model);

        OpenRouterChatStreamRequest request = buildStreamRequest(model, prompt, messages);

        return openRouterStreamService.stream(request, model)
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