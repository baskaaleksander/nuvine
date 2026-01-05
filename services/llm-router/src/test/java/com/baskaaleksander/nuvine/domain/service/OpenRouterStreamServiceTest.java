package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.LlmChunk;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatStreamRequest;
import com.baskaaleksander.nuvine.application.dto.OpenRouterStreamEvent;
import com.baskaaleksander.nuvine.domain.exception.ModelCircuitBreakerOpenException;
import com.baskaaleksander.nuvine.infrastructure.resilience.OpenRouterCircuitBreakerRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OpenRouterStreamServiceTest {

    @Mock
    private WebClient openRouterWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private OpenRouterCircuitBreakerRegistry circuitBreakerRegistry;

    private ObjectMapper objectMapper;
    private OpenRouterStreamService openRouterStreamService;
    private CircuitBreaker circuitBreaker;

    private String model;
    private OpenRouterChatStreamRequest request;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        circuitBreaker = registry.circuitBreaker("test-circuit-breaker");
        lenient().when(circuitBreakerRegistry.getCircuitBreaker(anyString())).thenReturn(circuitBreaker);

        openRouterStreamService = new OpenRouterStreamService(openRouterWebClient, objectMapper, circuitBreakerRegistry);

        model = "openai/gpt-4";
        request = new OpenRouterChatStreamRequest(
                model,
                List.of(new OpenRouterChatStreamRequest.Message("user", "Hello")),
                0.7,
                2048,
                true,
                new OpenRouterChatStreamRequest.StreamOptions(true)
        );
    }

    @SuppressWarnings("unchecked")
    private void setupWebClientMocks(Flux<String> responseFlux) {
        when(openRouterWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/chat/completions")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(MediaType.TEXT_EVENT_STREAM)).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(String.class)).thenReturn(responseFlux);
    }

    @Test
    void stream_validRequest_callsWebClient() {
        setupWebClientMocks(Flux.empty());

        openRouterStreamService.stream(request).blockLast();

        verify(openRouterWebClient).post();
        verify(requestBodyUriSpec).uri("/chat/completions");
        verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(requestBodySpec).accept(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    void handleSseData_contentDelta_emitsDeltaChunk() throws JsonProcessingException {
        OpenRouterStreamEvent.Delta delta = new OpenRouterStreamEvent.Delta("assistant", "Hello");
        OpenRouterStreamEvent.Choice choice = new OpenRouterStreamEvent.Choice(delta, 0, null);
        OpenRouterStreamEvent event = new OpenRouterStreamEvent("id-1", model, List.of(choice), null);
        String jsonData = objectMapper.writeValueAsString(event);

        setupWebClientMocks(Flux.just(jsonData));

        StepVerifier.create(openRouterStreamService.stream(request))
                .assertNext(chunk -> {
                    assertEquals("delta", chunk.type());
                    assertEquals("Hello", chunk.content());
                    assertEquals(model, chunk.model());
                    assertNull(chunk.tokensIn());
                    assertNull(chunk.tokensOut());
                })
                .verifyComplete();
    }

    @Test
    void handleSseData_usageStats_emitsUsageChunk() throws JsonProcessingException {
        OpenRouterStreamEvent.Usage usage = new OpenRouterStreamEvent.Usage(10, 20, 30);
        OpenRouterStreamEvent event = new OpenRouterStreamEvent("id-1", model, null, usage);
        String jsonData = objectMapper.writeValueAsString(event);

        setupWebClientMocks(Flux.just(jsonData));

        StepVerifier.create(openRouterStreamService.stream(request))
                .assertNext(chunk -> {
                    assertEquals("usage", chunk.type());
                    assertNull(chunk.content());
                    assertEquals(10, chunk.tokensIn());
                    assertEquals(20, chunk.tokensOut());
                    assertEquals(model, chunk.model());
                })
                .verifyComplete();
    }

    @Test
    void handleSseData_doneEvent_emitsDoneChunk() {
        setupWebClientMocks(Flux.just("[DONE]"));

        StepVerifier.create(openRouterStreamService.stream(request))
                .assertNext(chunk -> {
                    assertEquals("done", chunk.type());
                    assertNull(chunk.content());
                    assertNull(chunk.tokensIn());
                    assertNull(chunk.tokensOut());
                    assertNull(chunk.model());
                })
                .verifyComplete();
    }

    @Test
    void handleSseData_invalidJson_emitsErrorChunk() {
        setupWebClientMocks(Flux.just("invalid json {{{"));

        StepVerifier.create(openRouterStreamService.stream(request))
                .assertNext(chunk -> {
                    assertEquals("error", chunk.type());
                    assertNull(chunk.content());
                    assertNull(chunk.tokensIn());
                    assertNull(chunk.tokensOut());
                    assertNull(chunk.model());
                })
                .verifyComplete();
    }

    @Test
    void handleSseData_emptyOrNullData_skips() {
        setupWebClientMocks(Flux.just("", "   ", "[DONE]"));

        StepVerifier.create(openRouterStreamService.stream(request))
                .assertNext(chunk -> assertEquals("done", chunk.type()))
                .verifyComplete();
    }

    @Test
    void stream_circuitBreakerOpen_throwsModelCircuitBreakerOpenException() {
        setupWebClientMocks(Flux.just("test data"));
        circuitBreaker.transitionToOpenState();

        StepVerifier.create(openRouterStreamService.stream(request, model))
                .expectError(ModelCircuitBreakerOpenException.class)
                .verify();
    }

    @Test
    void stream_usesCircuitBreakerForModel() {
        setupWebClientMocks(Flux.just("[DONE]"));

        openRouterStreamService.stream(request, model).blockLast();

        verify(circuitBreakerRegistry).getCircuitBreaker(model);
    }
}
