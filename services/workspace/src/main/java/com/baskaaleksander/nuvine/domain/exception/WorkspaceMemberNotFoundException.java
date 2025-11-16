package com.baskaaleksander.nuvine.domain.exception;

public class WorkspaceMemberNotFoundException extends RuntimeException {
    public WorkspaceMemberNotFoundException(String message) {
        super(message);
    }
}
