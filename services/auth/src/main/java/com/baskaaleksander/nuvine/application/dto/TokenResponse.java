package com.baskaaleksander.nuvine.application.dto;

public record TokenResponse(
        String accessToken,
        Long expiresIn
) {
}
