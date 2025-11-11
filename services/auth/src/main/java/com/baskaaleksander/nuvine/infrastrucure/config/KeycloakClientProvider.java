package com.baskaaleksander.nuvine.infrastrucure.config;

import com.baskaaleksander.nuvine.application.dto.LoginRequest;
import com.baskaaleksander.nuvine.application.dto.KeycloakTokenResponse;
import com.baskaaleksander.nuvine.domain.exception.InvalidCredentialsException;
import com.baskaaleksander.nuvine.infrastrucure.client.KeycloakFeignClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
public class KeycloakClientProvider {

    private final KeycloakFeignClient feignClient;

    @Value("${keycloak.server-url}")
    private String serverUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;

    public Keycloak getInstance() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType("client_credentials")
                .build();
    }

    public KeycloakTokenResponse loginUser(LoginRequest request) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("username", request.email());
            form.add("password", request.password());
            form.add("scope", "openid profile email");
            return feignClient.getToken(
                    form
            );
        } catch (FeignException ex) {
            if (ex.status() == 401 | ex.status() == 400) {
                throw new InvalidCredentialsException("Invalid credentials");
            }
            throw new RuntimeException("Keycloak token request failed " + ex.getMessage());
        }
    }

    public KeycloakTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        return feignClient.refreshToken(form);
    }
}
