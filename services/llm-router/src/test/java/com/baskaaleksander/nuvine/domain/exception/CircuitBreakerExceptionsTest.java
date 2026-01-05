package com.baskaaleksander.nuvine.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerExceptionsTest {

    @Test
    void circuitBreakerOpenException_containsCorrectMessage() {
        CircuitBreakerOpenException exception = new CircuitBreakerOpenException("test-cb", "TestService");

        assertTrue(exception.getMessage().contains("test-cb"));
        assertTrue(exception.getMessage().contains("TestService"));
        assertEquals("test-cb", exception.getCircuitBreakerName());
        assertEquals("TestService", exception.getServiceName());
    }

    @Test
    void circuitBreakerOpenException_withCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        CircuitBreakerOpenException exception = new CircuitBreakerOpenException("test-cb", "TestService", cause);

        assertSame(cause, exception.getCause());
    }

    @Test
    void modelCircuitBreakerOpenException_containsModelName() {
        ModelCircuitBreakerOpenException exception = new ModelCircuitBreakerOpenException("gpt-4");

        assertEquals("gpt-4", exception.getModelName());
        assertTrue(exception.getMessage().contains("gpt-4"));
        assertEquals("OpenRouter", exception.getServiceName());
    }

    @Test
    void modelCircuitBreakerOpenException_sanitizesModelNameInCircuitBreakerName() {
        ModelCircuitBreakerOpenException exception = new ModelCircuitBreakerOpenException("openai/gpt-4-turbo");

        assertTrue(exception.getCircuitBreakerName().contains("openai_gpt-4-turbo"));
    }

    @Test
    void modelCircuitBreakerOpenException_withCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        ModelCircuitBreakerOpenException exception = new ModelCircuitBreakerOpenException("gpt-4", cause);

        assertSame(cause, exception.getCause());
        assertEquals("gpt-4", exception.getModelName());
    }

    @Test
    void embeddingCircuitBreakerOpenException_hasCorrectDefaults() {
        EmbeddingCircuitBreakerOpenException exception = new EmbeddingCircuitBreakerOpenException();

        assertTrue(exception.getMessage().contains("OpenAI Embeddings"));
        assertEquals("OpenAI Embeddings", exception.getServiceName());
        assertTrue(exception.getCircuitBreakerName().contains("embeddings"));
    }

    @Test
    void embeddingCircuitBreakerOpenException_withCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        EmbeddingCircuitBreakerOpenException exception = new EmbeddingCircuitBreakerOpenException(cause);

        assertSame(cause, exception.getCause());
    }
}
