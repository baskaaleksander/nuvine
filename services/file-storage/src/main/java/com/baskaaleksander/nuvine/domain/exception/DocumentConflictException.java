package com.baskaaleksander.nuvine.domain.exception;

public class DocumentConflictException extends RuntimeException {
    public DocumentConflictException(String message) {
        super(message);
    }
}
