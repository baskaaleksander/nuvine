package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.CreateDocumentRequest;
import com.baskaaleksander.nuvine.application.dto.UpdateDocumentRequest;
import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DocumentControllerIT extends BaseControllerIntegrationTest {

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
    void createDocument_asModerator_returnsDocument() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project");
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.MODERATOR);

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        CreateDocumentRequest request = new CreateDocumentRequest("Document A");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + project.getId() + "/documents",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Document A", response.getBody().get("name"));
        assertEquals(project.getId().toString(), response.getBody().get("projectId"));
        assertEquals(workspace.getId().toString(), response.getBody().get("workspaceId"));
    }

    @Test
    void createDocument_asViewer_returns403() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project");
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        CreateDocumentRequest request = new CreateDocumentRequest("Document A");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + project.getId() + "/documents",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getDocuments_asMember_returnsFilteredDocuments() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project");
        testData.createDocument(workspace.getId(), project.getId(), USER_1_ID, "Doc 1", DocumentStatus.UPLOADING);
        testData.createDocument(workspace.getId(), project.getId(), USER_1_ID, "Doc 2", DocumentStatus.FAILED);
        testData.createDocument(workspace.getId(), project.getId(), USER_1_ID, "Doc 3", DocumentStatus.UPLOADED);

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/projects/" + project.getId() + "/documents?status=UPLOADED&page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        List<?> content = (List<?>) response.getBody().get("content");
        assertNotNull(content);
        assertEquals(1, content.size());
    }

    @Test
    void getDocument_asMember_returnsDocument() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project");
        Document document = testData.createDocument(workspace.getId(), project.getId(), USER_1_ID, "Doc 1");

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/documents/" + document.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(document.getId().toString(), response.getBody().get("id"));
        assertEquals("Doc 1", response.getBody().get("name"));
    }

    @Test
    void getDocument_notMember_returns403() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project");
        Document document = testData.createDocument(workspace.getId(), project.getId(), USER_1_ID, "Doc 1");

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/documents/" + document.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void updateDocument_asModerator_returnsUpdatedDocument() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project");
        Document document = testData.createDocument(workspace.getId(), project.getId(), USER_1_ID, "Doc 1");
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.MODERATOR);

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        UpdateDocumentRequest request = new UpdateDocumentRequest("Updated Doc");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/documents/" + document.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Doc", response.getBody().get("name"));
    }

    @Test
    void updateDocument_asViewer_returns403() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project");
        Document document = testData.createDocument(workspace.getId(), project.getId(), USER_1_ID, "Doc 1");
        testData.addMemberToWorkspace(workspace.getId(), USER_2_ID, USER_2_EMAIL, WorkspaceRole.VIEWER);

        String jwt = jwtUtils.generateJwt(USER_2_ID, USER_2_EMAIL, List.of("ROLE_USER"));
        UpdateDocumentRequest request = new UpdateDocumentRequest("Updated Doc");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/documents/" + document.getId(),
                HttpMethod.PATCH,
                new HttpEntity<>(request, authHeaders(jwt)),
                Map.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deleteDocument_asOwner_returns204() {
        Workspace workspace = testData.createWorkspace("Workspace", USER_1_ID, USER_1_EMAIL);
        Project project = testData.createProject(workspace.getId(), "Project");
        Document document = testData.createDocument(workspace.getId(), project.getId(), USER_1_ID, "Doc 1");

        String jwt = jwtUtils.generateJwt(USER_1_ID, USER_1_EMAIL, List.of("ROLE_USER"));

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/documents/" + document.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(jwt)),
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
