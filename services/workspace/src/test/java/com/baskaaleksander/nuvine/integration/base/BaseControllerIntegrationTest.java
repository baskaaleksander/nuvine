package com.baskaaleksander.nuvine.integration.base;

import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class BaseControllerIntegrationTest extends BaseIntegrationTest {

    protected static WireMockServer wireMockServer;

    @Autowired
    protected TestRestTemplate restTemplate;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .dynamicPort()
                        .usingFilesUnderClasspath("wiremock")
        );
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
    }

    @DynamicPropertySource
    static void configureWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("application.config.auth-internal-url",
                () -> "http://localhost:" + wireMockServer.port() + "/api/v1/internal/auth");

        registry.add("keycloak.auth-server-url",
                () -> "http://localhost:" + wireMockServer.port() + "/realms/nuvine/protocol/openid-connect/token");
        registry.add("keycloak.client-id", () -> "workspace-service");
        registry.add("keycloak.client-secret", () -> "test-secret");

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:" + wireMockServer.port() + "/realms/nuvine");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + wireMockServer.port() + "/realms/nuvine/protocol/openid-connect/certs");
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
        setupDefaultStubs();
    }

    @AfterAll
    static void shutdownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    protected void setupDefaultStubs() {
        stubJwkEndpoint();
    }

    protected void stubJwkEndpoint() {
        String jwksResponse = String.format("""
                        {
                          "keys": [
                            {
                              "kty": "RSA",
                              "alg": "RS256",
                              "use": "sig",
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
                WireMock.get(WireMock.urlPathEqualTo("/realms/nuvine/protocol/openid-connect/certs"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(jwksResponse))
        );
    }

    protected HttpHeaders authHeaders(String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        return headers;
    }
}
