package com.baskaaleksander.nuvine.application.exception;

import com.baskaaleksander.nuvine.domain.exception.CircuitBreakerOpenException;
import com.baskaaleksander.nuvine.domain.exception.EmbeddingCircuitBreakerOpenException;
import com.baskaaleksander.nuvine.domain.exception.ErrorResponse;
import com.baskaaleksander.nuvine.domain.exception.ModelCircuitBreakerOpenException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/internal/llm/completions");
    }

    @Test
    void handleModelCircuitBreakerOpen_returns503WithRetryAfter() {
        ModelCircuitBreakerOpenException exception = new ModelCircuitBreakerOpenException("gpt-4");

        ResponseEntity<ErrorResponse> response = handler.handleModelCircuitBreakerOpen(exception, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().status());
        assertTrue(response.getBody().message().contains("gpt-4"));
    }

    @Test
    void handleModelCircuitBreakerOpen_includesCircuitBreakerDetails() {
        ModelCircuitBreakerOpenException exception = new ModelCircuitBreakerOpenException("claude-3-opus");

        ResponseEntity<ErrorResponse> response = handler.handleModelCircuitBreakerOpen(exception, request);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().details());
        GlobalExceptionHandler.CircuitBreakerDetails details =
                (GlobalExceptionHandler.CircuitBreakerDetails) response.getBody().details();
        assertEquals("claude-3-opus", details.modelName());
        assertEquals("OPEN", details.state());
    }

    @Test
    void handleEmbeddingCircuitBreakerOpen_returns503WithRetryAfter() {
        EmbeddingCircuitBreakerOpenException exception = new EmbeddingCircuitBreakerOpenException();

        ResponseEntity<ErrorResponse> response = handler.handleEmbeddingCircuitBreakerOpen(exception, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().status());
        assertTrue(response.getBody().message().contains("OpenAI Embeddings"));
    }

    @Test
    void handleEmbeddingCircuitBreakerOpen_includesCircuitBreakerDetails() {
        EmbeddingCircuitBreakerOpenException exception = new EmbeddingCircuitBreakerOpenException();

        ResponseEntity<ErrorResponse> response = handler.handleEmbeddingCircuitBreakerOpen(exception, request);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().details());
        GlobalExceptionHandler.CircuitBreakerDetails details =
                (GlobalExceptionHandler.CircuitBreakerDetails) response.getBody().details();
        assertNull(details.modelName());
        assertEquals("OPEN", details.state());
        assertTrue(details.circuitBreakerName().contains("embeddings"));
    }

    @Test
    void handleCircuitBreakerOpen_returns503() {
        CircuitBreakerOpenException exception = new CircuitBreakerOpenException("test-cb", "TestService");

        ResponseEntity<ErrorResponse> response = handler.handleCircuitBreakerOpen(exception, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().status());
    }

    @Test
    void handleGenericException_returns500() {
        Exception exception = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("Something went wrong", response.getBody().message());
        assertEquals("/api/v1/internal/llm/completions", response.getBody().path());
    }

    @Test
    void handleGenericException_includesTimestamp() {
        Exception exception = new RuntimeException("Error");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception, request);

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().timestamp());
    }
}
