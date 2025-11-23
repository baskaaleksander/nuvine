package com.baskaaleksander.nuvine.domain.exception;

public class IngestionJobNotFound extends RuntimeException {
    public IngestionJobNotFound(String message) {
        super(message);
    }
}
