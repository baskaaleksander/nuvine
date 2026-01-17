package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.WorkspaceCreateRequest;
import com.baskaaleksander.nuvine.application.dto.WorkspaceUpdateRequest;
import com.baskaaleksander.nuvine.domain.model.Workspace;
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

class WorkspaceControllerIT extends BaseControllerIntegrationTest {

    private static final String BASE_URL = "/api/v1/workspaces";

    @Autowired
    private TestDataBuilder testData;

    private JwtTestUtils jwtUtils;
    private WireMockStubs stubs;

    private static final UUID USER_1_ID = UUID.randomUUID();
    private static final String USER_1_EMAIL = "user1@example.com";

    private static final UUID USER_2_ID = UUID.randomUUID();
    private static final String USER_2_EMAIL = "user2@example.com";

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
    void createWorkspace_authenticated_returnsCreatedWorkspace() {
        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));
        WorkspaceCreateRequest request = new WorkspaceCreateRequest("My Workspace");

        org.springframework.http.HttpHeaders headers = authHeaders(jwt);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("id"));
        assertEquals("My Workspace", response.getBody().get("name"));
        assertEquals(USER_1_ID.toString(), response.getBody().get("ownerUserId"));
    }

    @Test
    void createWorkspace_unauthenticated_returns401() {
        WorkspaceCreateRequest request = new WorkspaceCreateRequest("My Workspace");

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                new HttpEntity<>(request),
                Map.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getWorkspaces_returnsPaginatedUserWorkspaces() {
        testData.createWorkspace("Workspace 1", USER_1_ID, USER_1_EMAIL);
        testData.createWorkspace("Workspace 2", USER_1_ID, USER_1_EMAIL);
        testData.createWorkspace("Other User Workspace", USER_2_ID, USER_2_EMAIL);

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<?> content = (List<?>) response.getBody().get("content");
        assertNotNull(content);
        assertEquals(2, content.size());
    }

    @Test
    void getWorkspaceById_asMember_returnsWorkspace() {
        Workspace workspace = testData.createWorkspace("Test Workspace", USER_1_ID, USER_1_EMAIL);
        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/" + workspace.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(workspace.getId().toString(), response.getBody().get("id"));
        assertEquals("Test Workspace", response.getBody().get("name"));
    }

    @Test
    void getWorkspaceById_notMember_returns403() {
        Workspace workspace = testData.createWorkspace("Test Workspace", USER_1_ID, USER_1_EMAIL);
        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/" + workspace.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }


    @Test
    void updateWorkspace_asOwner_returns204() {
        Workspace workspace = testData.createWorkspace("Original Name", USER_1_ID, USER_1_EMAIL);
        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));
        WorkspaceUpdateRequest request = new WorkspaceUpdateRequest("Updated Name");

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/" + workspace.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateWorkspace_asViewer_returns403() {
        Workspace workspace = testData.createWorkspace("Original Name", USER_1_ID, USER_1_EMAIL);
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        WorkspaceUpdateRequest request = new WorkspaceUpdateRequest("Updated Name");

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/" + workspace.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deleteWorkspace_asOwner_returns204() {
        Workspace workspace = testData.createWorkspace("To Be Deleted", USER_1_ID, USER_1_EMAIL);
        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/" + workspace.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }


    @Test
    void getMyMembership_asMember_returnsMemberInfo() {
        Workspace workspace = testData.createWorkspace("Test Workspace", USER_1_ID, USER_1_EMAIL);
        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/" + workspace.getId() + "/me",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(USER_1_ID.toString(), response.getBody().get("userId"));
        assertEquals("OWNER", response.getBody().get("role"));
    }
}
