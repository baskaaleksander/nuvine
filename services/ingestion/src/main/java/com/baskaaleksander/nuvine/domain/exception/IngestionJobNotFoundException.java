package com.baskaaleksander.nuvine.domain.exception;

public class IngestionJobNotFoundException extends RuntimeException {
    public IngestionJobNotFoundException(String message) {
        super(message);
    }
}
