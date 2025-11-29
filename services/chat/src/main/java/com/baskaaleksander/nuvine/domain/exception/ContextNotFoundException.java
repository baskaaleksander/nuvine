package com.baskaaleksander.nuvine.domain.exception;

public class ContextNotFoundException extends RuntimeException {
    public ContextNotFoundException(String message) {
        super(message);
    }
}
