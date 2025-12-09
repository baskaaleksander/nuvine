package com.baskaaleksander.nuvine.domain.exception;

//todo add global exception handler
public class CheckLimitNotFoundException extends RuntimeException {
    public CheckLimitNotFoundException(String message) {
        super(message);
    }
}
