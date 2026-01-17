package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.CreateProjectRequest;
import com.baskaaleksander.nuvine.application.dto.UpdateProjectRequest;
import com.baskaaleksander.nuvine.domain.model.Project;
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

class ProjectControllerIT extends BaseControllerIntegrationTest {

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
    void createProject_asOwner_returnsProject() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));
        CreateProjectRequest request = new CreateProjectRequest("Project A", "Description");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/projects",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Project A", response.getBody().get("name"));
        assertEquals("Description", response.getBody().get("description"));
        assertEquals(workspace.getId().toString(), response.getBody().get("workspaceId"));
    }

    @Test
    void createProject_asViewer_returns403() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        CreateProjectRequest request = new CreateProjectRequest("Project A", "Description");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/projects",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getProjects_asMember_returnsPaginatedProjects() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        testData.createProject(workspace.getId(), "Project 1");
        testData.createProject(workspace.getId(), "Project 2");

        Workspace otherWorkspace = testData.createWorkspace("Other", USER_2_ID, USER_2_EMAIL);
        testData.createProject(otherWorkspace.getId(), "Other Project");

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/workspaces/" + workspace.getId() + "/projects?page=0&size=10",
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
    void getProjectById_asMember_returnsProject() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project A");
        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + project.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(project.getId().toString(), response.getBody().get("id"));
        assertEquals("Project A", response.getBody().get("name"));
    }

    @Test
    void getProjectById_notMember_returns403() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project A");
        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + project.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void updateProject_asModerator_returns200() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.MODERATOR);
        Project project = testData.createProject(workspace.getId(), "Project A");

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        UpdateProjectRequest request = new UpdateProjectRequest("Updated Project", "Updated description");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/projects/" + project.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateProject_asViewer_returns403() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);
        Project project = testData.createProject(workspace.getId(), "Project A");

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        UpdateProjectRequest request = new UpdateProjectRequest("Updated Project", "Updated description");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/projects/" + project.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deleteProject_asOwner_returns204() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project A");
        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/projects/" + project.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
