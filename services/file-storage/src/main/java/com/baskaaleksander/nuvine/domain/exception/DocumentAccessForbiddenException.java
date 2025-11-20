package com.baskaaleksander.nuvine.domain.exception;

public class DocumentAccessForbiddenException extends RuntimeException {
    public DocumentAccessForbiddenException(String message) {
        super(message);
    }
}
