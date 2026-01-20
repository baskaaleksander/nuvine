package com.baskaaleksander.nuvine.application.exception;

import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.ErrorResponse;
import com.baskaaleksander.nuvine.domain.exception.IngestionJobConflictException;
import com.baskaaleksander.nuvine.domain.exception.IngestionJobNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.UnauthorizedDocumentAccessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IngestionJobConflictException.class)
    public ResponseEntity<ErrorResponse> handleIngestionJobConflictException(IngestionJobConflictException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                409,
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.status(409).body(errorResponse);
    }

    @ExceptionHandler(IngestionJobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIngestionJobNotFoundException(IngestionJobNotFoundException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                404,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.status(404).body(errorResponse);
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFoundException(DocumentNotFoundException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                404,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.status(404).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedDocumentAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedDocumentAccessException(UnauthorizedDocumentAccessException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                403,
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.status(403).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                500,
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.status(500).body(errorResponse);
    }
}
