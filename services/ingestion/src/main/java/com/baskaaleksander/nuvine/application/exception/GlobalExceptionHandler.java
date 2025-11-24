package com.baskaaleksander.nuvine.application.exception;

import com.baskaaleksander.nuvine.domain.exception.ErrorResponse;
import com.baskaaleksander.nuvine.domain.exception.IngestionJobConflictException;
import com.baskaaleksander.nuvine.domain.exception.IngestionJobNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Arrays;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        System.out.println(ex.getMessage());
        System.out.println(ex.getClass());
        System.out.println(Arrays.toString(ex.getStackTrace()));
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
