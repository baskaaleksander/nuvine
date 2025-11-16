package com.baskaaleksander.nuvine.infrastructure.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakClientCredentialsProperties {

    private String authServerUrl;
    private String clientId;
    private String clientSecret;

    public String getTokenUrl() {
        return authServerUrl;
    }
}
