package com.baskaaleksander.nuvine.application.exception;

import com.baskaaleksander.nuvine.domain.exception.CheckLimitNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.ContextNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.ErrorResponse;
import com.baskaaleksander.nuvine.domain.exception.RequestLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RequestLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRequestLimitExceededException(RequestLimitExceededException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                409,
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.status(409).body(errorResponse);
    }

    @ExceptionHandler(ContextNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContextNotFoundException(ContextNotFoundException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                404,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );

        return ResponseEntity.status(404).body(errorResponse);
    }

    @ExceptionHandler(CheckLimitNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCheckLimitNotFoundException(CheckLimitNotFoundException ex, HttpServletRequest request) {
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
