package com.baskaaleksander.nuvine.application.exception;

import com.baskaaleksander.nuvine.domain.exception.ErrorResponse;
import com.baskaaleksander.nuvine.domain.exception.InvalidWorkspaceNameException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkspaceMemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceMemberNotFoundException(WorkspaceMemberNotFoundException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                404,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WorkspaceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkspaceNotFoundException(WorkspaceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                404,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidWorkspaceNameException.class)
    public ResponseEntity<ErrorResponse> handleInvalidWorkspaceNameException(InvalidWorkspaceNameException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                400,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                400,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                400,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                errors,
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                403,
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
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
