package com.baskaaleksander.nuvine.domain.exception;

// todo handle that
public class InvalidEmailVerificationTokenException extends RuntimeException {
    public InvalidEmailVerificationTokenException(String message) {
        super(message);
    }
}
