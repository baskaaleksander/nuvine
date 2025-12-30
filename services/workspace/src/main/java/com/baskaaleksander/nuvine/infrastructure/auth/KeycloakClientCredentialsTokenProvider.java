package com.baskaaleksander.nuvine.infrastructure.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakClientCredentialsTokenProvider {

    private final KeycloakClientCredentialsProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedToken;
    private long expiresAt = 0;

    // todo: cache this
    public synchronized String getAccessToken() {
        long now = System.currentTimeMillis();

        if (cachedToken != null && now < expiresAt) {
            return cachedToken;
        }

        log.info("Fetching new Keycloak service-account token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(properties.getTokenUrl(), request, Map.class);

        String accessToken = (String) response.getBody().get("access_token");
        Integer expiresIn = (Integer) response.getBody().get("expires_in");

        this.cachedToken = accessToken;
        this.expiresAt = now + (expiresIn - 10) * 1000;

        return accessToken;
    }
}
