package com.baskaaleksander.nuvine.domain.exception;

public class EmailVerificationTokenNotFoundException extends RuntimeException {
    public EmailVerificationTokenNotFoundException(String message) {
        super(message);
    }
}
