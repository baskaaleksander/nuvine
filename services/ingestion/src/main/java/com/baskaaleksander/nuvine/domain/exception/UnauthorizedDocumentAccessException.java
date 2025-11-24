package com.baskaaleksander.nuvine.domain.exception;

public class UnauthorizedDocumentAccessException extends RuntimeException {
    public UnauthorizedDocumentAccessException(String message) {
        super(message);
    }
}
