package com.baskaaleksander.nuvine.domain.exception;

public class CheckLimitNotFoundException extends RuntimeException {
    public CheckLimitNotFoundException(String message) {
        super(message);
    }
}
