package com.baskaaleksander.nuvine.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockStubs {

    private final WireMockServer wireMockServer;

    public WireMockStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    // ============ LLM Router Service Stubs ============

    public void stubLlmRouterCompletion(String content, int tokensIn, int tokensOut, String model) {
        String responseBody = String.format("""
            {
                "content": "%s",
                "tokensIn": %d,
                "tokensOut": %d,
                "modelUsed": "%s"
            }
            """, escapeJson(content), tokensIn, tokensOut, model);

        wireMockServer.stubFor(
            post(urlPathEqualTo("/internal/llm/completions"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody))
        );
    }

    public void stubLlmRouterStreamingCompletion(List<String> chunks, int tokensIn, int tokensOut) {
        StringBuilder ndjsonBuilder = new StringBuilder();

        for (String chunk : chunks) {
            ndjsonBuilder.append(String.format(
                "{\"type\":\"delta\",\"content\":\"%s\",\"tokensIn\":0,\"tokensOut\":0}\n",
                escapeJson(chunk)
            ));
        }

        ndjsonBuilder.append(String.format(
            "{\"type\":\"usage\",\"content\":\"\",\"tokensIn\":%d,\"tokensOut\":%d}\n",
            tokensIn, tokensOut
        ));

        ndjsonBuilder.append("{\"type\":\"done\",\"content\":\"\",\"tokensIn\":0,\"tokensOut\":0}\n");

        wireMockServer.stubFor(
            post(urlPathEqualTo("/api/v1/internal/llm/completion/stream"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/x-ndjson")
                    .withBody(ndjsonBuilder.toString()))
        );
    }

    public void stubLlmRouterError(int status, String message) {
        String responseBody = String.format("""
            {
                "error": "%s"
            }
            """, escapeJson(message));

        wireMockServer.stubFor(
            post(urlPathEqualTo("/internal/llm/completions"))
                .willReturn(aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody))
        );
    }

    // ============ Vector Service Stubs ============

    public void stubVectorSearchWithResults(List<VectorMatch> matches) {
        StringBuilder matchesJson = new StringBuilder("[");
        for (int i = 0; i < matches.size(); i++) {
            VectorMatch match = matches.get(i);
            if (i > 0) matchesJson.append(",");
            matchesJson.append(String.format("""
                {
                    "documentId": "%s",
                    "page": %d,
                    "startOffset": %d,
                    "endOffset": %d,
                    "content": "%s",
                    "score": %f
                }
                """,
                match.documentId(),
                match.page(),
                match.startOffset(),
                match.endOffset(),
                escapeJson(match.content()),
                match.score()
            ));
        }
        matchesJson.append("]");

        String responseBody = String.format("""
            {
                "matches": %s
            }
            """, matchesJson);

        wireMockServer.stubFor(
            post(urlPathEqualTo("/internal/vector/search-by-text"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody))
        );
    }

    public void stubVectorSearchEmpty() {
        String responseBody = """
            {
                "matches": []
            }
            """;

        wireMockServer.stubFor(
            post(urlPathEqualTo("/internal/vector/search-by-text"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody))
        );
    }

    public void stubVectorServiceError(int status) {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/internal/vector/search-by-text"))
                .willReturn(aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Vector service error\"}"))
        );
    }

    // ============ Subscription Service Stubs ============

    public void stubSubscriptionCheckLimitApproved(
            BigDecimal estimatedCost,
            BigDecimal usedCredits,
            BigDecimal reservedCredits,
            BigDecimal limitCredits
    ) {
        String responseBody = String.format("""
            {
                "approved": true,
                "estimatedCost": %s,
                "usedCredits": %s,
                "reservedCredits": %s,
                "limitCredits": %s
            }
            """, estimatedCost, usedCredits, reservedCredits, limitCredits);

        wireMockServer.stubFor(
            post(urlPathEqualTo("/internal/billing/check-limit"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody))
        );
    }

    public void stubSubscriptionCheckLimitRejected(
            BigDecimal usedCredits,
            BigDecimal reservedCredits,
            BigDecimal estimatedCost,
            BigDecimal limitCredits
    ) {
        String responseBody = String.format("""
            {
                "approved": false,
                "estimatedCost": %s,
                "usedCredits": %s,
                "reservedCredits": %s,
                "limitCredits": %s
            }
            """, estimatedCost, usedCredits, reservedCredits, limitCredits);

        wireMockServer.stubFor(
            post(urlPathEqualTo("/internal/billing/check-limit"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody))
        );
    }

    public void stubSubscriptionCheckLimitNotFound() {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/internal/billing/check-limit"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Subscription not found\"}"))
        );
    }

    public void stubSubscriptionReleaseReservation() {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/internal/billing/release-reservation"))
                .willReturn(aResponse()
                    .withStatus(200))
        );
    }

    // ============ Workspace Service Stubs ============

    public void stubWorkspaceAccessGranted(UUID workspaceId) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/workspaces/" + workspaceId))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\": \"" + workspaceId + "\"}"))
        );
    }

    public void stubWorkspaceAccessDenied(UUID workspaceId) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/workspaces/" + workspaceId))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Access denied\"}"))
        );
    }

    public void stubProjectDocumentIds(UUID projectId, List<UUID> documentIds) {
        StringBuilder idsJson = new StringBuilder("[");
        for (int i = 0; i < documentIds.size(); i++) {
            if (i > 0) idsJson.append(",");
            idsJson.append("\"").append(documentIds.get(i)).append("\"");
        }
        idsJson.append("]");

        wireMockServer.stubFor(
            get(urlPathEqualTo("/internal/projects/" + projectId + "/document-ids"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(idsJson.toString()))
        );
    }

    // ============ Keycloak Stubs ============

    public void stubKeycloakToken() {
        String responseBody = """
            {
                "access_token": "test-service-token",
                "expires_in": 300,
                "token_type": "Bearer"
            }
            """;

        wireMockServer.stubFor(
            post(urlPathEqualTo("/realms/nuvine/protocol/openid-connect/token"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody))
        );
    }

    public void stubKeycloakJwks() {
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
            get(urlPathEqualTo("/realms/nuvine/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(jwksResponse))
        );
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    // ============ Helper Records ============

    public record VectorMatch(
        UUID documentId,
        int page,
        int startOffset,
        int endOffset,
        String content,
        float score
    ) {}
}
