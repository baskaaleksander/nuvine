package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.InvitationAction;
import com.baskaaleksander.nuvine.application.dto.InvitationResponseRequest;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberInviteTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvitationControllerIT extends BaseControllerIntegrationTest {

    private static final UUID USER_1_ID = UUID.randomUUID();
    private static final String USER_1_EMAIL = "user1@example.com";

    private static final UUID USER_2_ID = UUID.randomUUID();
    private static final String USER_2_EMAIL = "user2@example.com";

    @Autowired
    private TestDataBuilder testData;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceMemberInviteTokenRepository inviteTokenRepository;

    private JwtTestUtils jwtUtils;
    private WireMockStubs stubs;

    @BeforeEach
    void setUp() {
        testData.cleanUp();

        String issuer = "http://localhost:" + wireMockServer.port() + "/realms/nuvine";
        jwtUtils = new JwtTestUtils(issuer);
        stubs = new WireMockStubs(wireMockServer);

        stubs.stubKeycloakToken();
        stubs.stubAuthServiceGetUser(USER_1_ID, USER_1_EMAIL, "User", "One");
        stubs.stubAuthServiceGetUser(USER_2_ID, USER_2_EMAIL, "User", "Two");
        stubs.stubAuthServiceGetUserByEmail(USER_1_EMAIL, USER_1_ID, "User", "One");
        stubs.stubAuthServiceGetUserByEmail(USER_2_EMAIL, USER_2_ID, "User", "Two");
    }

    @Test
    void checkInvitationToken_validToken_returnsWorkspaceName() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        WorkspaceMember member = testData.createWorkspaceMember(
                workspace.getId(),
                null,
                USER_2_EMAIL,
                "Invitee",
                WorkspaceRole.VIEWER,
                WorkspaceMemberStatus.PENDING
        );

        WorkspaceMemberInviteToken inviteToken = testData.createInviteToken(member, "token-123");

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/workspaces/invitations/" + inviteToken.getToken() + "/check",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Workspace", response.getBody().get("workspaceName"));
    }

    @Test
    void respondToInvitation_accept_updatesMember() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        WorkspaceMember member = testData.createWorkspaceMember(
                workspace.getId(),
                null,
                USER_2_EMAIL,
                "Invitee",
                WorkspaceRole.VIEWER,
                WorkspaceMemberStatus.PENDING
        );

        WorkspaceMemberInviteToken inviteToken = testData.createInviteToken(member, "token-accept");

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        InvitationResponseRequest request = new InvitationResponseRequest(InvitationAction.ACCEPT);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/workspaces/invitations/" + inviteToken.getToken(),
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        WorkspaceMember updated = workspaceMemberRepository.findById(member.getId()).orElseThrow();
        assertEquals(WorkspaceMemberStatus.ACCEPTED, updated.getStatus());
        assertNotNull(updated.getUserId());
    }

    @Test
    void respondToInvitation_decline_marksDeleted() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        WorkspaceMember member = testData.createWorkspaceMember(
                workspace.getId(),
                null,
                USER_2_EMAIL,
                "Invitee",
                WorkspaceRole.VIEWER,
                WorkspaceMemberStatus.PENDING
        );

        WorkspaceMemberInviteToken inviteToken = testData.createInviteToken(member, "token-decline");

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        InvitationResponseRequest request = new InvitationResponseRequest(InvitationAction.DECLINE);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/workspaces/invitations/" + inviteToken.getToken(),
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        WorkspaceMember updated = workspaceMemberRepository.findById(member.getId()).orElseThrow();
        assertEquals(WorkspaceMemberStatus.REJECTED, updated.getStatus());
        assertTrue(updated.isDeleted());
    }
}
