package com.baskaaleksander.nuvine.domain.exception;

public class InvitationTokenNotFoundException extends RuntimeException {
    public InvitationTokenNotFoundException(String message) {
        super(message);
    }
}
