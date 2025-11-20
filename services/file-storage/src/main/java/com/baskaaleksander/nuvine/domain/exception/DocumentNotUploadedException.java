package com.baskaaleksander.nuvine.domain.exception;

public class DocumentNotUploadedException extends RuntimeException {
    public DocumentNotUploadedException(String message) {
        super(message);
    }
}
