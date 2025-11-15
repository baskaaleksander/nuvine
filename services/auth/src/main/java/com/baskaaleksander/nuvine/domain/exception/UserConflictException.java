package com.baskaaleksander.nuvine.domain.exception;

// todo handle that
public class UserConflictException extends RuntimeException {
    public UserConflictException(String message) {
        super(message);
    }
}
