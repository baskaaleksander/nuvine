package com.baskaaleksander.nuvine.domain.exception;

public class IngestionJobConflictException extends RuntimeException {
    public IngestionJobConflictException(String message) {
        super(message);
    }
}
