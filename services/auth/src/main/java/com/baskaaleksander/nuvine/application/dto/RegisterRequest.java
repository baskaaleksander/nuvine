package com.baskaaleksander.nuvine.application.dto;

public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String password
) {
}
