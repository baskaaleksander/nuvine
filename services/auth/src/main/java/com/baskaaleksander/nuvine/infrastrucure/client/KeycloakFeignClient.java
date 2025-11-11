package com.baskaaleksander.nuvine.infrastrucure.client;

import com.baskaaleksander.nuvine.application.dto.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "keycloak-auth-client",
        url= "${keycloak-server-url}/realms/${keycloak.realm}/protocol/openid-connect"
)
public interface KeycloakFeignClient {

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    TokenResponse getToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "scope", defaultValue = "openid profile email") String scope
    );
}
