package com.baskaaleksander.nuvine.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.List;
import java.util.UUID;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class WireMockStubs {

    private final WireMockServer wireMockServer;
    private final String realm;

    public WireMockStubs(WireMockServer wireMockServer, String realm) {
        this.wireMockServer = wireMockServer;
        this.realm = realm;
    }

    public void stubJwks() {
        String jwksJson = String.format("""
            {
                "keys": [
                    {
                        "kty": "RSA",
                        "use": "sig",
                        "alg": "RS256",
                        "kid": "%s",
                        "n": "%s",
                        "e": "%s"
                    }
                ]
            }
            """,
                JwtTestUtils.getKeyId(),
                JwtTestUtils.getModulusBase64Url(),
                JwtTestUtils.getExponentBase64Url()
        );

        wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/realms/" + realm + "/protocol/openid-connect/certs"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(jwksJson))
        );
    }

    public void stubClientCredentialsToken() {
        wireMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/realms/" + realm + "/protocol/openid-connect/token"))
                        .withRequestBody(WireMock.containing("grant_type=client_credentials"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                    {
                                        "access_token": "mock-client-credentials-token",
                                        "expires_in": 300,
                                        "token_type": "Bearer"
                                    }
                                    """))
        );
    }

    public void stubPasswordToken(String accessToken, String refreshToken) {
        wireMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/realms/" + realm + "/protocol/openid-connect/token"))
                        .withRequestBody(WireMock.containing("grant_type=password"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(tokenResponse(accessToken, refreshToken)))
        );
    }

    public void stubPasswordToken(String email, String accessToken, String refreshToken) {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        wireMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/realms/" + realm + "/protocol/openid-connect/token"))
                        .withRequestBody(WireMock.containing("grant_type=password"))
                        .withRequestBody(WireMock.matching(".*username=(" + encodedEmail + "|" + email + ").*"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(tokenResponse(accessToken, refreshToken)))
        );
    }

    public void stubRefreshToken(String refreshToken, String accessToken, String newRefreshToken) {
        wireMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/realms/" + realm + "/protocol/openid-connect/token"))
                        .withRequestBody(WireMock.containing("grant_type=refresh_token"))
                        .withRequestBody(WireMock.containing("refresh_token=" + refreshToken))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(tokenResponse(accessToken, newRefreshToken)))
        );
    }

    public void stubUserSearchEmpty() {
        wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/users"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("[]"))
        );
    }

    public void stubUserSearchResult(UUID userId, String email) {
        String response = String.format("""
            [
              {
                "id": "%s",
                "username": "%s",
                "email": "%s"
              }
            ]
            """, userId, email, email);

        wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/users"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(response))
        );
    }

    public void stubCreateUser(UUID userId) {
        wireMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/users"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(201)
                                .withHeader("Location", "/admin/realms/" + realm + "/users/" + userId))
        );
    }

    public void stubUserGet(UUID userId, String email, String firstName, String lastName, boolean emailVerified) {
        String response = String.format("""
            {
              "id": "%s",
              "username": "%s",
              "email": "%s",
              "firstName": "%s",
              "lastName": "%s",
              "emailVerified": %s,
              "enabled": true
            }
            """, userId, email, email, firstName, lastName, emailVerified);

        wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/users/" + userId))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(response))
        );
    }

    public void stubUserUpdate(UUID userId) {
        wireMockServer.stubFor(
                WireMock.put(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/users/" + userId))
                        .willReturn(WireMock.aResponse()
                                .withStatus(204))
        );
    }

    public void stubResetPassword(String userId) {
        wireMockServer.stubFor(
                WireMock.put(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/users/" + userId + "/reset-password"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(204))
        );
    }

    public void stubRoleLookup(String roleName) {
        String response = String.format("""
            {
              "id": "%s",
              "name": "%s"
            }
            """, UUID.randomUUID(), roleName);

        wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/roles/" + roleName))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(response))
        );
    }

    public void stubRoleMappingAdd(UUID userId) {
        wireMockServer.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(204))
        );
    }

    public void stubRoleMappingGet(UUID userId, List<String> roles) {
        String realmMappings = roles.stream()
                .map(role -> String.format("{\"id\":\"%s\",\"name\":\"%s\"}", UUID.randomUUID(), role))
                .collect(Collectors.joining(",", "[", "]"));

        wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/users/" + userId + "/role-mappings"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"realmMappings\":" + realmMappings + ",\"clientMappings\":{}}"))
        );

        wireMockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(realmMappings))
        );
    }

    private String tokenResponse(String accessToken, String refreshToken) {
        return String.format("""
            {
                "access_token": "%s",
                "refresh_token": "%s",
                "expires_in": 300,
                "token_type": "Bearer",
                "scope": "openid profile email"
            }
            """, accessToken, refreshToken);
    }
}
