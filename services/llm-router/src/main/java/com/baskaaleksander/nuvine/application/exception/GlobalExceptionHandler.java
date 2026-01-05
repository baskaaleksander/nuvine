package com.baskaaleksander.nuvine.application.exception;

import com.baskaaleksander.nuvine.domain.exception.CircuitBreakerOpenException;
import com.baskaaleksander.nuvine.domain.exception.EmbeddingCircuitBreakerOpenException;
import com.baskaaleksander.nuvine.domain.exception.ErrorResponse;
import com.baskaaleksander.nuvine.domain.exception.ModelCircuitBreakerOpenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ModelCircuitBreakerOpenException.class)
    public ResponseEntity<ErrorResponse> handleModelCircuitBreakerOpen(
            ModelCircuitBreakerOpenException ex,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                503,
                ex.getMessage(),
                new CircuitBreakerDetails(
                        ex.getCircuitBreakerName(),
                        ex.getModelName(),
                        "OPEN"
                ),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "60")
                .body(errorResponse);
    }

    @ExceptionHandler(EmbeddingCircuitBreakerOpenException.class)
    public ResponseEntity<ErrorResponse> handleEmbeddingCircuitBreakerOpen(
            EmbeddingCircuitBreakerOpenException ex,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                503,
                ex.getMessage(),
                new CircuitBreakerDetails(
                        ex.getCircuitBreakerName(),
                        null,
                        "OPEN"
                ),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "60")
                .body(errorResponse);
    }

    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(
            CircuitBreakerOpenException ex,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                503,
                ex.getMessage(),
                new CircuitBreakerDetails(
                        ex.getCircuitBreakerName(),
                        null,
                        "OPEN"
                ),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "60")
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                500,
                ex.getMessage(),
                null,
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    public record CircuitBreakerDetails(
            String circuitBreakerName,
            String modelName,
            String state
    ) {}
}
