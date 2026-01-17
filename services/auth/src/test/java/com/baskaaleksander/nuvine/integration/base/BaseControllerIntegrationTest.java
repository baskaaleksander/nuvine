package com.baskaaleksander.nuvine.integration.base;

import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
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
public abstract class BaseControllerIntegrationTest extends BaseKafkaIntegrationTest {

    protected static final WireMockServer wireMockServer;
    protected static final WireMockStubs wireMockStubs;

    static {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .dynamicPort()
                        .usingFilesUnderClasspath("wiremock")
        );
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
        wireMockStubs = new WireMockStubs(wireMockServer, "nuvine");
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
        registry.add("keycloak.server-url",
                () -> "http://localhost:" + wireMockServer.port());
        registry.add("keycloak.realm", () -> "nuvine");
        registry.add("keycloak.client-id", () -> "auth-service");
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
        wireMockStubs.stubJwks();
        wireMockStubs.stubClientCredentialsToken();
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
