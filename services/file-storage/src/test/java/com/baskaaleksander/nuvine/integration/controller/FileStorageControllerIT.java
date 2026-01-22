package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.UploadUrlRequest;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageControllerIT extends BaseControllerIntegrationTest {

    private WireMockStubs stubs;
    private JwtTestUtils jwtUtils;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        stubs = new WireMockStubs(wireMockServer);
        jwtUtils = new JwtTestUtils("http://localhost:" + wireMockServer.port() + "/realms/nuvine");
        stubs.stubKeycloakToken();
    }

    @Nested
    @DisplayName("POST /api/v1/files/upload-url")
    class GenerateUploadUrl {

        @Test
        @DisplayName("Should return presigned URL when document is in UPLOADING status")
        void generateUploadUrl_validRequest_returnsPresignedUrl() {
            UUID documentId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();

            stubs.stubGetDocumentUploading(documentId, projectId, workspaceId, "test-doc.pdf", USER_ID);

            UploadUrlRequest request = new UploadUrlRequest(
                    documentId.toString(),
                    "application/pdf",
                    1024L
            );

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/upload-url",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("url")).isNotNull();
            assertThat(response.getBody().get("url").toString()).contains("test-bucket");
            assertThat(response.getBody().get("method")).isEqualTo("PUT");
        }

        @Test
        @DisplayName("Should return 404 when document not found")
        void generateUploadUrl_documentNotFound_returns404() {
            UUID documentId = UUID.randomUUID();

            stubs.stubGetDocumentNotFound(documentId);

            UploadUrlRequest request = new UploadUrlRequest(
                    documentId.toString(),
                    "application/pdf",
                    1024L
            );

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/upload-url",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 403 when access is forbidden")
        void generateUploadUrl_accessForbidden_returns403() {
            UUID documentId = UUID.randomUUID();

            stubs.stubGetDocumentForbidden(documentId);

            UploadUrlRequest request = new UploadUrlRequest(
                    documentId.toString(),
                    "application/pdf",
                    1024L
            );

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/upload-url",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should return 409 when document is already uploaded")
        void generateUploadUrl_documentAlreadyUploaded_returns409() {
            UUID documentId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();
            String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;

            stubs.stubGetDocumentUploaded(documentId, projectId, workspaceId, "test-doc.pdf",
                    storageKey, "application/pdf", 1024L, USER_ID);

            UploadUrlRequest request = new UploadUrlRequest(
                    documentId.toString(),
                    "application/pdf",
                    1024L
            );

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/upload-url",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should return 500 when workspace service returns error")
        void generateUploadUrl_workspaceServiceError_returns500() {
            UUID documentId = UUID.randomUUID();

            stubs.stubGetDocumentServerError(documentId);

            UploadUrlRequest request = new UploadUrlRequest(
                    documentId.toString(),
                    "application/pdf",
                    1024L
            );

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/upload-url",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should return 401 when no authentication provided")
        void generateUploadUrl_noAuthentication_returns401() {
            UUID documentId = UUID.randomUUID();

            UploadUrlRequest request = new UploadUrlRequest(
                    documentId.toString(),
                    "application/pdf",
                    1024L
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/upload-url",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 401 when token is expired")
        void generateUploadUrl_expiredToken_returns401() {
            UUID documentId = UUID.randomUUID();

            UploadUrlRequest request = new UploadUrlRequest(
                    documentId.toString(),
                    "application/pdf",
                    1024L
            );

            HttpHeaders headers = authHeaders(jwtUtils.generateExpiredJwt(USER_ID, USER_EMAIL));
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/upload-url",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/files/{documentId}/download-url")
    class GetDownloadUrl {

        @Test
        @DisplayName("Should return presigned URL when document is UPLOADED")
        void getDownloadUrl_uploadedDocument_returnsPresignedUrl() {
            UUID documentId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();
            String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;

            stubs.stubGetDocumentUploaded(documentId, projectId, workspaceId, "test-doc.pdf",
                    storageKey, "application/pdf", 1024L, USER_ID);

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/" + documentId + "/download-url",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("url")).isNotNull();
            assertThat(response.getBody().get("url").toString()).contains("test-bucket");
        }

        @Test
        @DisplayName("Should return presigned URL when document is PROCESSED")
        void getDownloadUrl_processedDocument_returnsPresignedUrl() {
            UUID documentId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();
            String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;

            stubs.stubGetDocumentProcessed(documentId, projectId, workspaceId, "test-doc.pdf",
                    storageKey, "application/pdf", 1024L, USER_ID);

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/" + documentId + "/download-url",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("url")).isNotNull();
        }

        @Test
        @DisplayName("Should return 409 when document is still UPLOADING")
        void getDownloadUrl_uploadingDocument_returns409() {
            UUID documentId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();

            stubs.stubGetDocumentUploading(documentId, projectId, workspaceId, "test-doc.pdf", USER_ID);

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/" + documentId + "/download-url",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should return 409 when document is FAILED")
        void getDownloadUrl_failedDocument_returns409() {
            UUID documentId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();

            stubs.stubGetDocumentFailed(documentId, projectId, workspaceId, "test-doc.pdf", USER_ID);

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/" + documentId + "/download-url",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Should return 404 when document not found")
        void getDownloadUrl_documentNotFound_returns404() {
            UUID documentId = UUID.randomUUID();

            stubs.stubGetDocumentNotFound(documentId);

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/" + documentId + "/download-url",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 403 when access is forbidden")
        void getDownloadUrl_accessForbidden_returns403() {
            UUID documentId = UUID.randomUUID();

            stubs.stubGetDocumentForbidden(documentId);

            HttpHeaders headers = authHeaders(jwtUtils.generateJwt(USER_ID, USER_EMAIL));

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/" + documentId + "/download-url",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should return 401 when no authentication provided")
        void getDownloadUrl_noAuthentication_returns401() {
            UUID documentId = UUID.randomUUID();

            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/v1/files/" + documentId + "/download-url",
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
