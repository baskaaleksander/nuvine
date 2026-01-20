package com.baskaaleksander.nuvine.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.List;
import java.util.stream.Collectors;

public class WireMockStubs {

    private final WireMockServer wireMockServer;

    public WireMockStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void stubLlmRouterEmbeddings(List<Float> embedding) {
        stubLlmRouterEmbeddingsBatch(List.of(embedding));
    }

    public void stubLlmRouterEmbeddingsBatch(List<List<Float>> embeddings) {
        String embeddingsJson = embeddings.stream()
                .map(emb -> emb.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",", "[", "]")))
                .collect(Collectors.joining(",", "[", "]"));

        String responseBody = String.format("""
            {
                "embeddings": %s,
                "usedModel": "text-embedding-3-small"
            }
            """, embeddingsJson);

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/internal/llm/embeddings"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody))
        );
    }

    public void stubLlmRouterEmbeddingsError(int status, String message) {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/internal/llm/embeddings"))
                .willReturn(WireMock.aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody(String.format("{\"error\":\"%s\"}", message)))
        );
    }

    public void stubKeycloakToken() {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/realms/nuvine/protocol/openid-connect/token"))
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

    public void stubKeycloakJwks() {
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
            WireMock.get(WireMock.urlPathEqualTo("/realms/nuvine/protocol/openid-connect/certs"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(jwksJson))
        );
    }
}
