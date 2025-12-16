package com.baskaaleksander.nuvine.domain.exception;

public class InvitationTokenExpiredException extends RuntimeException {
    public InvitationTokenExpiredException(String message) {
        super(message);
    }
}
