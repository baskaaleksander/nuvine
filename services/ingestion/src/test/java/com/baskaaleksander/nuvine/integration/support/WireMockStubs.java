package com.baskaaleksander.nuvine.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.time.Instant;
import java.util.UUID;

public class WireMockStubs {

    private final WireMockServer wireMockServer;

    public WireMockStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void stubWorkspaceGetDocument(UUID documentId, UUID projectId, UUID workspaceId,
                                         String name, String storageKey, String mimeType,
                                         long sizeBytes, UUID createdBy) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(documentResponseJson(documentId, projectId, workspaceId, name,
                            storageKey, mimeType, sizeBytes, createdBy)))
        );
    }

    public void stubWorkspaceDocumentNotFound(UUID documentId) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(WireMock.aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Document not found\"}"))
        );
    }

    public void stubWorkspaceDocumentUnauthorized(UUID documentId) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(WireMock.aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Unauthorized\"}"))
        );
    }

    public void stubWorkspaceServiceError(UUID documentId) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(WireMock.aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Internal server error\"}"))
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

    private String documentResponseJson(UUID documentId, UUID projectId, UUID workspaceId,
                                        String name, String storageKey, String mimeType,
                                        long sizeBytes, UUID createdBy) {
        return String.format("""
            {
                "id": "%s",
                "projectId": "%s",
                "workspaceId": "%s",
                "name": "%s",
                "status": "UPLOADED",
                "storageKey": "%s",
                "mimeType": "%s",
                "sizeBytes": %d,
                "createdBy": "%s",
                "createdAt": "%s"
            }
            """, documentId, projectId, workspaceId, name, storageKey, mimeType, sizeBytes, createdBy, Instant.now());
    }
}
