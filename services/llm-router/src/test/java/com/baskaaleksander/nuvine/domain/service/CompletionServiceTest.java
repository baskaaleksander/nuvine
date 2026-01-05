package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CompletionResponse;
import com.baskaaleksander.nuvine.application.dto.LlmChunk;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatRequest;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatResponse;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatStreamRequest;
import com.baskaaleksander.nuvine.domain.exception.ModelCircuitBreakerOpenException;
import com.baskaaleksander.nuvine.infrastructure.ai.client.OpenRouterClient;
import com.baskaaleksander.nuvine.infrastructure.resilience.OpenRouterCircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CompletionServiceTest {

    @Mock
    private OpenRouterClient client;

    @Mock
    private OpenRouterStreamService openRouterStreamService;

    @Mock
    private OpenRouterCircuitBreakerRegistry circuitBreakerRegistry;

    private CompletionService completionService;
    private CircuitBreaker circuitBreaker;

    private String model;
    private String prompt;
    private OpenRouterChatResponse mockResponse;

    @BeforeEach
    void setUp() {
        model = "openai/gpt-4";
        prompt = "Hello, how are you?";

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        circuitBreaker = registry.circuitBreaker("test-circuit-breaker");
        lenient().when(circuitBreakerRegistry.getCircuitBreaker(anyString())).thenReturn(circuitBreaker);

        completionService = new CompletionService(client, openRouterStreamService, circuitBreakerRegistry);

        OpenRouterChatResponse.Choice.Message message = new OpenRouterChatResponse.Choice.Message("assistant", "I'm fine, thank you!");
        OpenRouterChatResponse.Choice choice = new OpenRouterChatResponse.Choice(0, message, "stop");
        OpenRouterChatResponse.Usage usage = new OpenRouterChatResponse.Usage(20, 10, 30, null, null);
        mockResponse = new OpenRouterChatResponse("chatcmpl-123", "chat.completion", 1234567890L, model, List.of(choice), usage);
    }

    @Test
    void call_validRequest_returnsCompletion() {
        when(client.createChatCompletion(any(OpenRouterChatRequest.class))).thenReturn(mockResponse);

        CompletionResponse result = completionService.call(model, prompt, null);

        assertEquals("I'm fine, thank you!", result.content());
        assertEquals(10, result.tokensIn());
        assertEquals(20, result.tokensOut());
        assertEquals(model, result.modelUsed());
        verify(client).createChatCompletion(any(OpenRouterChatRequest.class));
    }

    @Test
    void call_buildsCorrectRequestMessages() {
        when(client.createChatCompletion(any(OpenRouterChatRequest.class))).thenReturn(mockResponse);

        List<OpenRouterChatStreamRequest.Message> existingMessages = new ArrayList<>();
        existingMessages.add(new OpenRouterChatStreamRequest.Message("system", "You are a helpful assistant."));
        existingMessages.add(new OpenRouterChatStreamRequest.Message("user", "Previous message"));

        completionService.call(model, prompt, existingMessages);

        ArgumentCaptor<OpenRouterChatRequest> captor = ArgumentCaptor.forClass(OpenRouterChatRequest.class);
        verify(client).createChatCompletion(captor.capture());
        
        OpenRouterChatRequest capturedRequest = captor.getValue();
        assertEquals(model, capturedRequest.model());
        assertEquals(3, capturedRequest.messages().size());
        assertEquals("system", capturedRequest.messages().get(0).role());
        assertEquals("user", capturedRequest.messages().get(1).role());
        assertEquals("user", capturedRequest.messages().get(2).role());
        assertEquals(prompt, capturedRequest.messages().get(2).content());
        assertEquals(0.7, capturedRequest.temperature());
        assertEquals(2048, capturedRequest.max_tokens());
        assertFalse(capturedRequest.stream());
    }

    @Test
    void call_nullMessages_createsNewList() {
        when(client.createChatCompletion(any(OpenRouterChatRequest.class))).thenReturn(mockResponse);

        completionService.call(model, prompt, null);

        ArgumentCaptor<OpenRouterChatRequest> captor = ArgumentCaptor.forClass(OpenRouterChatRequest.class);
        verify(client).createChatCompletion(captor.capture());
        
        OpenRouterChatRequest capturedRequest = captor.getValue();
        assertEquals(1, capturedRequest.messages().size());
        assertEquals("user", capturedRequest.messages().get(0).role());
        assertEquals(prompt, capturedRequest.messages().get(0).content());
    }

    @Test
    void callStream_validRequest_returnsFlux() {
        LlmChunk chunk1 = new LlmChunk("delta", "Hello", null, null, model);
        LlmChunk chunk2 = new LlmChunk("delta", " World", null, null, model);
        LlmChunk chunk3 = new LlmChunk("done", null, null, null, null);

        when(openRouterStreamService.stream(any(OpenRouterChatStreamRequest.class), eq(model)))
                .thenReturn(Flux.just(chunk1, chunk2, chunk3));

        Flux<LlmChunk> result = completionService.callStream(model, prompt, null);

        StepVerifier.create(result)
                .expectNext(chunk1)
                .expectNext(chunk2)
                .expectNext(chunk3)
                .verifyComplete();

        verify(openRouterStreamService).stream(any(OpenRouterChatStreamRequest.class), eq(model));
    }

    @Test
    void callStream_buildsCorrectStreamRequest() {
        when(openRouterStreamService.stream(any(OpenRouterChatStreamRequest.class), eq(model)))
                .thenReturn(Flux.empty());

        List<OpenRouterChatStreamRequest.Message> existingMessages = new ArrayList<>();
        existingMessages.add(new OpenRouterChatStreamRequest.Message("system", "Be helpful."));

        completionService.callStream(model, prompt, existingMessages).blockLast();

        ArgumentCaptor<OpenRouterChatStreamRequest> captor = ArgumentCaptor.forClass(OpenRouterChatStreamRequest.class);
        verify(openRouterStreamService).stream(captor.capture(), eq(model));

        OpenRouterChatStreamRequest capturedRequest = captor.getValue();
        assertEquals(model, capturedRequest.model());
        assertEquals(2, capturedRequest.messages().size());
        assertEquals("system", capturedRequest.messages().get(0).role());
        assertEquals("user", capturedRequest.messages().get(1).role());
        assertEquals(prompt, capturedRequest.messages().get(1).content());
        assertEquals(0.7, capturedRequest.temperature());
        assertEquals(2048, capturedRequest.maxTokens());
        assertTrue(capturedRequest.stream());
        assertNotNull(capturedRequest.streamOptions());
        assertTrue(capturedRequest.streamOptions().includeUsage());
    }

    @Test
    void call_circuitBreakerOpen_throwsModelCircuitBreakerOpenException() {
        circuitBreaker.transitionToOpenState();

        assertThrows(ModelCircuitBreakerOpenException.class,
                () -> completionService.call(model, prompt, null));

        verify(client, never()).createChatCompletion(any());
    }

    @Test
    void call_usesCircuitBreakerForModel() {
        when(client.createChatCompletion(any(OpenRouterChatRequest.class))).thenReturn(mockResponse);

        completionService.call(model, prompt, null);

        verify(circuitBreakerRegistry).getCircuitBreaker(model);
    }
}
