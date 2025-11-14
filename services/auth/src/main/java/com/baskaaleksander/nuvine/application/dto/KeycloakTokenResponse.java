package com.baskaaleksander.nuvine.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;


public record KeycloakTokenResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("expires_in")
        Long expiresIn,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("scope")
        String scope
) {

}