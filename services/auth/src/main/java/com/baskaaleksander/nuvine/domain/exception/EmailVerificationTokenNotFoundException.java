package com.baskaaleksander.nuvine.domain.exception;

// todo handle that
public class EmailVerificationTokenNotFoundException extends RuntimeException {
    public EmailVerificationTokenNotFoundException(String message) {
        super(message);
    }
}
