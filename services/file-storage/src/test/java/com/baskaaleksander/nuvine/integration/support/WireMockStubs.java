package com.baskaaleksander.nuvine.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockStubs {

    private final WireMockServer wireMockServer;

    public WireMockStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    // Document with UPLOADING status
    public void stubGetDocumentUploading(UUID documentId, UUID projectId, UUID workspaceId, String name, UUID createdBy) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(documentResponseJson(documentId, projectId, workspaceId, name, "UPLOADING", null, null, null, createdBy)))
        );
    }

    // Document with UPLOADED status
    public void stubGetDocumentUploaded(UUID documentId, UUID projectId, UUID workspaceId, String name,
                                        String storageKey, String mimeType, Long sizeBytes, UUID createdBy) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(documentResponseJson(documentId, projectId, workspaceId, name, "UPLOADED", storageKey, mimeType, sizeBytes, createdBy)))
        );
    }

    // Document with PROCESSED status
    public void stubGetDocumentProcessed(UUID documentId, UUID projectId, UUID workspaceId, String name,
                                         String storageKey, String mimeType, Long sizeBytes, UUID createdBy) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(documentResponseJson(documentId, projectId, workspaceId, name, "PROCESSED", storageKey, mimeType, sizeBytes, createdBy)))
        );
    }

    // Document with FAILED status
    public void stubGetDocumentFailed(UUID documentId, UUID projectId, UUID workspaceId, String name, UUID createdBy) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(documentResponseJson(documentId, projectId, workspaceId, name, "FAILED", null, null, null, createdBy)))
        );
    }

    // Document not found
    public void stubGetDocumentNotFound(UUID documentId) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Document not found\"}"))
        );
    }

    // Document access forbidden
    public void stubGetDocumentForbidden(UUID documentId) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Access forbidden\"}"))
        );
    }

    // Workspace service error
    public void stubGetDocumentServerError(UUID documentId) {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/api/v1/internal/documents/" + documentId))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Internal server error\"}"))
        );
    }

    // Upload completed success
    public void stubUploadCompletedSuccess(UUID documentId) {
        wireMockServer.stubFor(
            patch(urlPathEqualTo("/api/v1/internal/documents/" + documentId + "/upload-completed"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{}"))
        );
    }

    // Upload completed - document not found
    public void stubUploadCompletedNotFound(UUID documentId) {
        wireMockServer.stubFor(
            patch(urlPathEqualTo("/api/v1/internal/documents/" + documentId + "/upload-completed"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Document not found\"}"))
        );
    }

    // Upload completed - server error
    public void stubUploadCompletedServerError(UUID documentId) {
        wireMockServer.stubFor(
            patch(urlPathEqualTo("/api/v1/internal/documents/" + documentId + "/upload-completed"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Internal server error\"}"))
        );
    }

    // Keycloak token endpoint
    public void stubKeycloakToken() {
        wireMockServer.stubFor(
            post(urlPathEqualTo("/realms/nuvine/protocol/openid-connect/token"))
                .willReturn(aResponse()
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

    // Keycloak JWKS endpoint
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
            get(urlPathEqualTo("/realms/nuvine/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(jwksJson))
        );
    }

    // Verification methods
    public void verifyUploadCompletedCalled(UUID documentId, String storageKey, String mimeType, Long sizeBytes) {
        wireMockServer.verify(
            patchRequestedFor(urlPathEqualTo("/api/v1/internal/documents/" + documentId + "/upload-completed"))
                .withRequestBody(matchingJsonPath("$.storageKey", equalTo(storageKey)))
                .withRequestBody(matchingJsonPath("$.mimeType", equalTo(mimeType)))
                .withRequestBody(matchingJsonPath("$.sizeBytes", equalTo(String.valueOf(sizeBytes))))
        );
    }

    public void verifyUploadCompletedNotCalled(UUID documentId) {
        wireMockServer.verify(0,
            patchRequestedFor(urlPathEqualTo("/api/v1/internal/documents/" + documentId + "/upload-completed"))
        );
    }

    public void verifyGetDocumentCalled(UUID documentId) {
        wireMockServer.verify(
            getRequestedFor(urlPathEqualTo("/api/v1/internal/documents/" + documentId))
        );
    }

    private String documentResponseJson(UUID documentId, UUID projectId, UUID workspaceId, String name,
                                        String status, String storageKey, String mimeType, Long sizeBytes, UUID createdBy) {
        String storageKeyJson = storageKey != null ? "\"" + storageKey + "\"" : "null";
        String mimeTypeJson = mimeType != null ? "\"" + mimeType + "\"" : "null";
        String sizeBytesJson = sizeBytes != null ? String.valueOf(sizeBytes) : "null";

        return String.format("""
            {
                "id": "%s",
                "projectId": "%s",
                "workspaceId": "%s",
                "name": "%s",
                "status": "%s",
                "storageKey": %s,
                "mimeType": %s,
                "sizeBytes": %s,
                "createdBy": "%s",
                "createdAt": "%s"
            }
            """, documentId, projectId, workspaceId, name, status, storageKeyJson, mimeTypeJson, sizeBytesJson, createdBy, Instant.now().toString());
    }
}
