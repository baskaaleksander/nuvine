package com.baskaaleksander.nuvine.infrastructure.ai.service;

import com.baskaaleksander.nuvine.application.dto.EmbeddingApiRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingApiResponse;
import com.baskaaleksander.nuvine.domain.exception.EmbeddingCircuitBreakerOpenException;
import com.baskaaleksander.nuvine.infrastructure.ai.client.OpenAIEmbeddingClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

@Service
@Slf4j
public class OpenAIEmbeddingService {

    private final OpenAIEmbeddingClient client;
    private final CircuitBreaker circuitBreaker;

    @Value("${openai.embedding-model}")
    private String embeddingModel;

    public OpenAIEmbeddingService(
            OpenAIEmbeddingClient client,
            @Qualifier("openaiEmbeddingsCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.client = client;
        this.circuitBreaker = circuitBreaker;
    }

    public List<List<Float>> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        EmbeddingApiRequest requestBody = new EmbeddingApiRequest(
                embeddingModel,
                texts
        );

        Supplier<EmbeddingApiResponse> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, () -> client.createEmbedding(requestBody));

        EmbeddingApiResponse response;
        try {
            response = decoratedSupplier.get();
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker OPEN for OpenAI Embeddings");
            throw new EmbeddingCircuitBreakerOpenException(e);
        }

        if (response == null || response.data() == null) {
            throw new IllegalStateException("Empty response from OpenAI embeddings API");
        }

        return response.data()
                .stream()
                .sorted(Comparator.comparingInt(EmbeddingApiResponse.EmbeddingData::index))
                .map(EmbeddingApiResponse.EmbeddingData::embedding)
                .toList();
    }
}
