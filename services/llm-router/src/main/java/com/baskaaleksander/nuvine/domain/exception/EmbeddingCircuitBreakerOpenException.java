package com.baskaaleksander.nuvine.domain.exception;

public class EmbeddingCircuitBreakerOpenException extends CircuitBreakerOpenException {

    public EmbeddingCircuitBreakerOpenException() {
        super("openai-embeddings-global", "OpenAI Embeddings");
    }

    public EmbeddingCircuitBreakerOpenException(Throwable cause) {
        super("openai-embeddings-global", "OpenAI Embeddings", cause);
    }

    @Override
    public String getMessage() {
        return "Circuit breaker is OPEN for OpenAI Embeddings API. The service has experienced too many failures. Please try again later.";
    }
}
