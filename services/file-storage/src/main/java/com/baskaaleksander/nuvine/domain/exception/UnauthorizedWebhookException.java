package com.baskaaleksander.nuvine.domain.exception;

public class UnauthorizedWebhookException extends RuntimeException {
    public UnauthorizedWebhookException(String message) {
        super(message);
    }
}
