package com.baskaaleksander.nuvine.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.UUID;

public class WireMockStubs {

    private final WireMockServer wireMockServer;

    public WireMockStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void stubAuthServiceGetUser(UUID userId, String email, String firstName, String lastName) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/internal/auth/users/" + userId))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(userResponseJson(userId, email, firstName, lastName)))
        );
    }

    public void stubAuthServiceGetUserByEmail(String email, UUID userId, String firstName, String lastName) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/internal/auth/users/email"))
                .withQueryParam("email", WireMock.equalTo(email))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(userResponseJson(userId, email, firstName, lastName)))
        );
    }

    public void stubAuthServiceUserNotFound(UUID userId) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/internal/auth/users/" + userId))
                .willReturn(WireMock.aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"User not found\"}"))
        );
    }

    public void stubAuthServiceUserNotFoundByEmail(String email) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/internal/auth/users/email"))
                .withQueryParam("email", WireMock.equalTo(email))
                .willReturn(WireMock.aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"User not found\"}"))
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

    public void stubAuthServiceError(UUID userId) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/v1/internal/auth/users/" + userId))
                .willReturn(WireMock.aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Internal server error\"}"))
        );
    }

    public void stubWorkspaceServiceGetWorkspace(UUID workspaceId, UUID ownerId, String name, String billingTier) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/internal/workspaces/" + workspaceId))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(workspaceResponseJson(workspaceId, ownerId, name, billingTier)))
        );
    }

    public void stubWorkspaceServiceUpdateBillingTier(UUID workspaceId) {
        wireMockServer.stubFor(
            WireMock.patch(WireMock.urlPathEqualTo("/internal/workspaces/" + workspaceId + "/billing-tier"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json"))
        );
    }

    public void stubWorkspaceServiceNotFound(UUID workspaceId) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/internal/workspaces/" + workspaceId))
                .willReturn(WireMock.aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Workspace not found\"}"))
        );
    }

    public void stubWorkspaceServiceGetWorkspaceMember(UUID workspaceId, UUID userId, String role) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/workspaces/" + workspaceId + "/members/me"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(workspaceMemberResponseJson(userId, workspaceId, role)))
        );
    }

    public void stubWorkspaceServiceMemberNotFound(UUID workspaceId) {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/workspaces/" + workspaceId + "/members/me"))
                .willReturn(WireMock.aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"Member not found\"}"))
        );
    }

    private String userResponseJson(UUID userId, String email, String firstName, String lastName) {
        return String.format("""
            {
                "id": "%s",
                "email": "%s",
                "firstName": "%s",
                "lastName": "%s"
            }
            """, userId, email, firstName, lastName);
    }

    private String workspaceResponseJson(UUID workspaceId, UUID ownerId, String name, String billingTier) {
        return String.format("""
            {
                "id": "%s",
                "ownerId": "%s",
                "name": "%s",
                "billingTier": "%s"
            }
            """, workspaceId, ownerId, name, billingTier);
    }

    private String workspaceMemberResponseJson(UUID userId, UUID workspaceId, String role) {
        return String.format("""
            {
                "id": "%s",
                "userId": "%s",
                "workspaceId": "%s",
                "role": "%s",
                "createdAt": "%s"
            }
            """, UUID.randomUUID(), userId, workspaceId, role, java.time.Instant.now());
    }
}
