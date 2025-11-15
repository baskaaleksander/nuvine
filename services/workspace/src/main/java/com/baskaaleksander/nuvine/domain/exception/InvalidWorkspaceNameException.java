package com.baskaaleksander.nuvine.domain.exception;

public class InvalidWorkspaceNameException extends RuntimeException {
    public InvalidWorkspaceNameException(String message) {
        super(message);
    }
}
