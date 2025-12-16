package com.baskaaleksander.nuvine.domain.exception;

public class InvitationEmailMismatchException extends RuntimeException {
    public InvitationEmailMismatchException(String message) {
        super(message);
    }
}
