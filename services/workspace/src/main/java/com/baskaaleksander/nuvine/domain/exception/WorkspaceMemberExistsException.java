package com.baskaaleksander.nuvine.domain.exception;

public class WorkspaceMemberExistsException extends RuntimeException {
    public WorkspaceMemberExistsException(String message) {
        super(message);
    }
}
