package com.baskaaleksander.nuvine.domain.exception;

public class WorkspaceRoleConflictException extends RuntimeException {
    public WorkspaceRoleConflictException(String message) {
        super(message);
    }
}
