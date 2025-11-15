package com.baskaaleksander.nuvine.application.dto;

public record EmailChangeRequest(
        String password,
        String email
) {
}
