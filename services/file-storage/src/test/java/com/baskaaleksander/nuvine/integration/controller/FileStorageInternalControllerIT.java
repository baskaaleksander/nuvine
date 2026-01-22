package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

class FileStorageInternalControllerIT extends BaseControllerIntegrationTest {

    private WireMockStubs stubs;

    private static final String WEBHOOK_SECRET = "test-webhook-secret";

    @BeforeEach
    void setUp() {
        stubs = new WireMockStubs(wireMockServer);
        stubs.stubKeycloakToken();
    }

    @Disabled("MinIO webhook integration requires external MinIO configuration script")
    @Test
    @DisplayName("Should notify workspace service when valid MinIO event received")
    void handleMinioEvent_validEvent_notifiesWorkspaceService() {
        UUID documentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;
        String mimeType = "application/pdf";
        long sizeBytes = 1024L;

        stubs.stubUploadCompletedSuccess(documentId);

        String minioEvent = createMinioEvent("test-bucket", storageKey, sizeBytes, mimeType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + WEBHOOK_SECRET);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/internal/file-storage/events",
                HttpMethod.POST,
                new HttpEntity<>(minioEvent, headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(5, SECONDS).untilAsserted(() ->
            stubs.verifyUploadCompletedCalled(documentId, storageKey, mimeType, sizeBytes)
        );
    }

    @Disabled("MinIO webhook integration requires external MinIO configuration script")
    @Test
    @DisplayName("Should decode URL-encoded storage key and process correctly")
    void handleMinioEvent_urlEncodedKey_decodesAndProcesses() {
        UUID documentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;
        String encodedKey = URLEncoder.encode(storageKey, StandardCharsets.UTF_8);
        String mimeType = "application/pdf";
        long sizeBytes = 2048L;

        stubs.stubUploadCompletedSuccess(documentId);

        String minioEvent = createMinioEvent("test-bucket", encodedKey, sizeBytes, mimeType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + WEBHOOK_SECRET);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/internal/file-storage/events",
                HttpMethod.POST,
                new HttpEntity<>(minioEvent, headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(5, SECONDS).untilAsserted(() ->
            stubs.verifyUploadCompletedCalled(documentId, storageKey, mimeType, sizeBytes)
        );
    }

    @Test
    @DisplayName("Should not process event when webhook secret is invalid")
    void handleMinioEvent_invalidSecret_doesNotProcessEvent() {
        UUID documentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;

        String minioEvent = createMinioEvent("test-bucket", storageKey, 1024L, "application/pdf");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer wrong-secret");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/internal/file-storage/events",
                HttpMethod.POST,
                new HttpEntity<>(minioEvent, headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        stubs.verifyUploadCompletedNotCalled(documentId);
    }

    @Test
    @DisplayName("Should not process event when Authorization header is missing")
    void handleMinioEvent_missingAuthHeader_doesNotProcessEvent() {
        UUID documentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;

        String minioEvent = createMinioEvent("test-bucket", storageKey, 1024L, "application/pdf");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // No Authorization header

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/internal/file-storage/events",
                HttpMethod.POST,
                new HttpEntity<>(minioEvent, headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        stubs.verifyUploadCompletedNotCalled(documentId);
    }

    @Test
    @DisplayName("Should not call workspace service when storage key format is invalid")
    void handleMinioEvent_invalidKeyFormat_doesNotCallWorkspaceService() {
        String invalidStorageKey = "invalid/key/format";

        String minioEvent = createMinioEvent("test-bucket", invalidStorageKey, 1024L, "application/pdf");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + WEBHOOK_SECRET);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/internal/file-storage/events",
                HttpMethod.POST,
                new HttpEntity<>(minioEvent, headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // No upload-completed call should be made for any document
    }

    @Disabled("MinIO webhook integration requires external MinIO configuration script")
    @Test
    @DisplayName("Should return OK when workspace service returns 500 (fire-and-forget)")
    void handleMinioEvent_workspaceServiceError_returnsOkButLogsError() {
        UUID documentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;

        stubs.stubUploadCompletedServerError(documentId);

        String minioEvent = createMinioEvent("test-bucket", storageKey, 1024L, "application/pdf");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + WEBHOOK_SECRET);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/internal/file-storage/events",
                HttpMethod.POST,
                new HttpEntity<>(minioEvent, headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Disabled("MinIO webhook integration requires external MinIO configuration script")
    @Test
    @DisplayName("Should return OK when workspace service returns 404 (fire-and-forget)")
    void handleMinioEvent_workspaceServiceNotFound_returnsOkButLogsError() {
        UUID documentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;

        stubs.stubUploadCompletedNotFound(documentId);

        String minioEvent = createMinioEvent("test-bucket", storageKey, 1024L, "application/pdf");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + WEBHOOK_SECRET);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/internal/file-storage/events",
                HttpMethod.POST,
                new HttpEntity<>(minioEvent, headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Disabled("MinIO webhook integration requires external MinIO configuration script")
    @Test
    @DisplayName("Should process different content types correctly")
    void handleMinioEvent_differentContentTypes_processesCorrectly() {
        UUID documentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;
        String mimeType = "image/png";
        long sizeBytes = 4096L;

        stubs.stubUploadCompletedSuccess(documentId);

        String minioEvent = createMinioEvent("test-bucket", storageKey, sizeBytes, mimeType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + WEBHOOK_SECRET);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/internal/file-storage/events",
                HttpMethod.POST,
                new HttpEntity<>(minioEvent, headers),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(5, SECONDS).untilAsserted(() ->
            stubs.verifyUploadCompletedCalled(documentId, storageKey, mimeType, sizeBytes)
        );
    }

    @Disabled("MinIO webhook integration requires external MinIO configuration script")
    @Test
    @DisplayName("Should not require JWT authentication for webhook endpoint")
    void handleMinioEvent_noJwtRequired_endpointIsPermitAll() {
        UUID documentId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        String storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;

        stubs.stubUploadCompletedSuccess(documentId);

        String minioEvent = createMinioEvent("test-bucket", storageKey, 1024L, "application/pdf");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + WEBHOOK_SECRET);
        // No JWT, just webhook secret

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/internal/file-storage/events",
                HttpMethod.POST,
                new HttpEntity<>(minioEvent, headers),
                Void.class
        );

        // Endpoint should be accessible without JWT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        await().atMost(5, SECONDS).untilAsserted(() ->
            stubs.verifyUploadCompletedCalled(documentId, storageKey, "application/pdf", 1024L)
        );
    }

    private String createMinioEvent(String bucket, String key, long size, String contentType) {
        return String.format("""
            {
              "Records": [
                {
                  "eventVersion": "2.0",
                  "eventSource": "minio:s3",
                  "eventName": "s3:ObjectCreated:Put",
                  "s3": {
                    "bucket": {
                      "name": "%s"
                    },
                    "object": {
                      "key": "%s",
                      "size": %d,
                      "contentType": "%s"
                    }
                  }
                }
              ]
            }
            """, bucket, key, size, contentType);
    }
}
