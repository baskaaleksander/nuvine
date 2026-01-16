package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.InviteWorkspaceMemberRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberRoleRequest;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberStatus;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkspaceMemberControllerIT extends BaseControllerIntegrationTest {

    private static final UUID USER_1_ID = UUID.randomUUID();
    private static final String USER_1_EMAIL = "user1@example.com";

    private static final UUID USER_2_ID = UUID.randomUUID();
    private static final String USER_2_EMAIL = "user2@example.com";

    @Autowired
    private TestDataBuilder testData;

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
    void getWorkspaceMembers_asMember_returnsMembers() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/members",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, ((List<?>) response.getBody().get("members")).size());
        assertEquals(2, ((Number) response.getBody().get("count")).intValue());
    }

    @Test
    void getWorkspaceMember_asMember_returnsMember() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/members/me",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(USER_1_ID.toString(), response.getBody().get("userId"));
    }

    @Test
    void removeWorkspaceMember_self_returns204() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/members/me",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void updateWorkspaceMemberRole_asOwner_returns200() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));
        WorkspaceMemberRoleRequest request = new WorkspaceMemberRoleRequest(WorkspaceRole.MODERATOR);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/members/" + USER_2_ID,
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateWorkspaceMemberRole_asViewer_returns403() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        WorkspaceMemberRoleRequest request = new WorkspaceMemberRoleRequest(WorkspaceRole.MODERATOR);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/members/" + USER_1_ID,
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void removeWorkspaceMember_asOwner_returns200() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/members/" + USER_2_ID,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void inviteWorkspaceMember_asOwner_returns201() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));
        InviteWorkspaceMemberRequest request = new InviteWorkspaceMemberRequest("invitee@example.com", WorkspaceRole.VIEWER);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/members/invite",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }
}
