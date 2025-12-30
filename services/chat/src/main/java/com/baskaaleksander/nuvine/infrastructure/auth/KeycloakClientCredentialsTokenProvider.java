package com.baskaaleksander.nuvine.infrastructure.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static com.baskaaleksander.nuvine.infrastructure.config.CacheConfiguration.KEYCLOAK_TOKEN_CACHE;


@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakClientCredentialsTokenProvider {

    private final KeycloakClientCredentialsProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable(cacheNames = KEYCLOAK_TOKEN_CACHE, key = "'service-account-token'")
    public String getAccessToken() {
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

        return accessToken;
    }
}
