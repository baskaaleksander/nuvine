package com.baskaaleksander.nuvine.infrastructure.ai.service;

import com.baskaaleksander.nuvine.application.dto.EmbeddingApiRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingApiResponse;
import com.baskaaleksander.nuvine.domain.exception.EmbeddingCircuitBreakerOpenException;
import com.baskaaleksander.nuvine.infrastructure.ai.client.OpenAIEmbeddingClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAIEmbeddingServiceTest {

    @Mock
    private OpenAIEmbeddingClient client;

    private OpenAIEmbeddingService service;
    private CircuitBreaker circuitBreaker;

    private List<Float> embedding1;
    private List<Float> embedding2;

    @BeforeEach
    void setUp() {
        embedding1 = List.of(0.1f, 0.2f, 0.3f);
        embedding2 = List.of(0.4f, 0.5f, 0.6f);

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        circuitBreaker = registry.circuitBreaker("test-embeddings-circuit-breaker");

        service = new OpenAIEmbeddingService(client, circuitBreaker);
        ReflectionTestUtils.setField(service, "embeddingModel", "text-embedding-3-small");
    }

    @Test
    void embed_validTexts_returnsEmbeddings() {
        List<String> texts = List.of("text1", "text2");

        EmbeddingApiResponse.EmbeddingData data1 = new EmbeddingApiResponse.EmbeddingData(0, embedding1);
        EmbeddingApiResponse.EmbeddingData data2 = new EmbeddingApiResponse.EmbeddingData(1, embedding2);
        EmbeddingApiResponse response = new EmbeddingApiResponse("text-embedding-3-small", List.of(data1, data2));

        when(client.createEmbedding(any(EmbeddingApiRequest.class))).thenReturn(response);

        List<List<Float>> result = service.embed(texts);

        assertEquals(2, result.size());
        assertEquals(embedding1, result.get(0));
        assertEquals(embedding2, result.get(1));
        verify(client).createEmbedding(any(EmbeddingApiRequest.class));
    }

    @Test
    void embed_sortsResponseByIndex() {
        List<String> texts = List.of("text1", "text2");

        EmbeddingApiResponse.EmbeddingData data1 = new EmbeddingApiResponse.EmbeddingData(1, embedding2);
        EmbeddingApiResponse.EmbeddingData data2 = new EmbeddingApiResponse.EmbeddingData(0, embedding1);
        EmbeddingApiResponse response = new EmbeddingApiResponse("text-embedding-3-small", List.of(data1, data2));

        when(client.createEmbedding(any(EmbeddingApiRequest.class))).thenReturn(response);

        List<List<Float>> result = service.embed(texts);

        assertEquals(embedding1, result.get(0));
        assertEquals(embedding2, result.get(1));
    }

    @Test
    void embed_nullResponse_throwsIllegalStateException() {
        List<String> texts = List.of("text1");

        when(client.createEmbedding(any(EmbeddingApiRequest.class))).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.embed(texts));

        assertEquals("Empty response from OpenAI embeddings API", exception.getMessage());
    }

    @Test
    void embed_emptyTexts_returnsEmptyList() {
        List<List<Float>> resultEmpty = service.embed(List.of());
        List<List<Float>> resultNull = service.embed(null);

        assertTrue(resultEmpty.isEmpty());
        assertTrue(resultNull.isEmpty());
        verifyNoInteractions(client);
    }

    @Test
    void embed_circuitBreakerOpen_throwsEmbeddingCircuitBreakerOpenException() {
        circuitBreaker.transitionToOpenState();

        List<String> texts = List.of("text1");

        assertThrows(EmbeddingCircuitBreakerOpenException.class,
                () -> service.embed(texts));

        verify(client, never()).createEmbedding(any());
    }

    @Test
    void embed_circuitBreakerClosed_callsClient() {
        List<String> texts = List.of("text1");

        EmbeddingApiResponse.EmbeddingData data = new EmbeddingApiResponse.EmbeddingData(0, embedding1);
        EmbeddingApiResponse response = new EmbeddingApiResponse("text-embedding-3-small", List.of(data));
        when(client.createEmbedding(any(EmbeddingApiRequest.class))).thenReturn(response);

        List<List<Float>> result = service.embed(texts);

        assertEquals(1, result.size());
        verify(client).createEmbedding(any(EmbeddingApiRequest.class));
    }
}
