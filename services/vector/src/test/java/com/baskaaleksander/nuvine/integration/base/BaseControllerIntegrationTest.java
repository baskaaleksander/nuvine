package com.baskaaleksander.nuvine.integration.base;

import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Import(BaseControllerIntegrationTest.TestSecurityConfig.class)
public abstract class BaseControllerIntegrationTest extends BaseIntegrationTest {

    protected static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .dynamicPort()
                        .usingFilesUnderClasspath("wiremock")
        );
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (wireMockServer.isRunning()) {
                wireMockServer.stop();
            }
        }));
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("application.config.api-base-url",
                () -> "http://localhost:" + wireMockServer.port());

        registry.add("keycloak.auth-server-url",
                () -> "http://localhost:" + wireMockServer.port() + "/realms/nuvine/protocol/openid-connect/token");
        registry.add("keycloak.client-id", () -> "vector-service");
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

    @Configuration
    static class TestSecurityConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withJwkSetUri(
                    "http://localhost:" + wireMockServer.port() + "/realms/nuvine/protocol/openid-connect/certs"
            ).build();
        }
    }
}
