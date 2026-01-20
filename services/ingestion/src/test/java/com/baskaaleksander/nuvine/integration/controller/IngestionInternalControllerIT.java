package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.IngestionJobConciseResponse;
import com.baskaaleksander.nuvine.application.dto.IngestionJobResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.S3TestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionInternalControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private IngestionJobRepository ingestionJobRepository;

    @Autowired
    private S3Client s3Client;

    @Value("${s3.bucket-name}")
    private String bucketName;

    private TestDataBuilder testDataBuilder;
    private WireMockStubs wireMockStubs;
    private S3TestUtils s3TestUtils;
    private JwtTestUtils jwtTestUtils;

    private UUID workspaceId;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        testDataBuilder = new TestDataBuilder(ingestionJobRepository);
        wireMockStubs = new WireMockStubs(wireMockServer);
        s3TestUtils = new S3TestUtils(s3Client, bucketName);
        jwtTestUtils = new JwtTestUtils("http://localhost:" + wireMockServer.port() + "/realms/nuvine");

        s3TestUtils.ensureBucketExists();
        testDataBuilder.cleanUp();

        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        wireMockStubs.stubKeycloakToken();
    }

    @AfterEach
    void tearDown() {
        s3TestUtils.cleanBucket();
        testDataBuilder.cleanUp();
    }

    @Nested
    @DisplayName("GET /api/v1/internal/ingestion/jobs")
    class GetAllJobs {

        @Test
        @DisplayName("should return jobs with INTERNAL_SERVICE role")
        void shouldReturnJobsWithInternalServiceRole() {
            testDataBuilder.createQueuedJob(workspaceId, projectId, UUID.randomUUID(), "test/key1.txt", "text/plain");
            testDataBuilder.createQueuedJob(workspaceId, projectId, UUID.randomUUID(), "test/key2.txt", "text/plain");

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<PagedResponse<IngestionJobConciseResponse>> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(jwt)),
                    new ParameterizedTypeReference<>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(2);
        }

        @Test
        @DisplayName("should filter by workspaceId")
        void shouldFilterByWorkspaceId() {
            UUID otherWorkspaceId = UUID.randomUUID();
            testDataBuilder.createQueuedJob(workspaceId, projectId, UUID.randomUUID(), "test/key1.txt", "text/plain");
            testDataBuilder.createQueuedJob(workspaceId, projectId, UUID.randomUUID(), "test/key2.txt", "text/plain");
            testDataBuilder.createQueuedJob(otherWorkspaceId, projectId, UUID.randomUUID(), "test/key3.txt", "text/plain");

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<PagedResponse<IngestionJobConciseResponse>> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs?workspaceId=" + workspaceId,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(jwt)),
                    new ParameterizedTypeReference<>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(2);
        }

        @Test
        @DisplayName("should filter by projectId")
        void shouldFilterByProjectId() {
            UUID otherProjectId = UUID.randomUUID();
            testDataBuilder.createQueuedJob(workspaceId, projectId, UUID.randomUUID(), "test/key1.txt", "text/plain");
            testDataBuilder.createQueuedJob(workspaceId, projectId, UUID.randomUUID(), "test/key2.txt", "text/plain");
            testDataBuilder.createQueuedJob(workspaceId, otherProjectId, UUID.randomUUID(), "test/key3.txt", "text/plain");

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<PagedResponse<IngestionJobConciseResponse>> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs?projectId=" + projectId,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(jwt)),
                    new ParameterizedTypeReference<>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(2);
        }

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() {
            testDataBuilder.createQueuedJob(workspaceId, projectId, UUID.randomUUID(), "test/key1.txt", "text/plain");
            testDataBuilder.createProcessingJob(workspaceId, projectId, UUID.randomUUID(), "test/key2.txt", "text/plain", IngestionStage.FETCH);
            testDataBuilder.createCompletedJob(workspaceId, projectId, UUID.randomUUID());

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<PagedResponse<IngestionJobConciseResponse>> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs?status=QUEUED",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(jwt)),
                    new ParameterizedTypeReference<>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(1);
            assertThat(response.getBody().content().stream().findFirst().get().status()).isEqualTo(IngestionStatus.QUEUED);
        }

        @Test
        @DisplayName("should return 401 without token")
        void shouldReturn401WithoutToken() {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should return 403 with USER role")
        void shouldReturn403WithUserRole() {
            String jwt = jwtTestUtils.generateUserJwt(UUID.randomUUID(), "user@test.com");

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(jwt)),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/internal/ingestion/jobs/{documentId}")
    class GetJobByDocumentId {

        @Test
        @DisplayName("should return job by document ID")
        void shouldReturnJobByDocumentId() {
            UUID documentId = UUID.randomUUID();
            testDataBuilder.createQueuedJob(workspaceId, projectId, documentId, "test/key.txt", "text/plain");

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<IngestionJobResponse> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs/" + documentId,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(jwt)),
                    IngestionJobResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().documentId()).isEqualTo(documentId);
            assertThat(response.getBody().workspaceId()).isEqualTo(workspaceId);
            assertThat(response.getBody().projectId()).isEqualTo(projectId);
            assertThat(response.getBody().status()).isEqualTo(IngestionStatus.QUEUED);
        }

        @Test
        @DisplayName("should return 404 for non-existent document")
        void shouldReturn404ForNonExistentDocument() {
            UUID nonExistentDocumentId = UUID.randomUUID();

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs/" + nonExistentDocumentId,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(jwt)),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/internal/ingestion/jobs/{documentId}/start")
    class StartJob {

        @Test
        @DisplayName("should start job successfully")
        void shouldStartJobSuccessfully() {
            UUID documentId = UUID.randomUUID();
            UUID createdBy = UUID.randomUUID();
            String storageKey = "test/" + documentId + "/document.txt";

            s3TestUtils.uploadTestDocument(storageKey, "Test document content for ingestion.");

            wireMockStubs.stubWorkspaceGetDocument(
                    documentId, projectId, workspaceId, "test.txt",
                    storageKey, "text/plain", 100L, createdBy
            );

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs/" + documentId + "/start",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(jwt)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should return 404 when document not found in workspace")
        void shouldReturn404WhenDocumentNotFoundInWorkspace() {
            UUID documentId = UUID.randomUUID();

            wireMockStubs.stubWorkspaceDocumentNotFound(documentId);

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs/" + documentId + "/start",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(jwt)),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should return 403 when unauthorized")
        void shouldReturn403WhenUnauthorized() {
            UUID documentId = UUID.randomUUID();

            wireMockStubs.stubWorkspaceDocumentUnauthorized(documentId);

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs/" + documentId + "/start",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(jwt)),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/internal/ingestion/jobs/{documentId}/retry")
    class RetryJob {

        @Test
        @DisplayName("should retry failed job successfully")
        void shouldRetryFailedJobSuccessfully() {
            UUID documentId = UUID.randomUUID();
            UUID createdBy = UUID.randomUUID();
            String storageKey = "failed/" + documentId + "/file.txt";

            testDataBuilder.createFailedJob(workspaceId, projectId, documentId, "Previous error", IngestionStage.PARSE);

            s3TestUtils.uploadTestDocument(storageKey, "Test content for retry.");

            wireMockStubs.stubWorkspaceGetDocument(
                    documentId, projectId, workspaceId, "test.txt",
                    storageKey, "text/plain", 100L, createdBy
            );

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<Void> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs/" + documentId + "/retry",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(jwt)),
                    Void.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should return 409 when job not in FAILED status")
        void shouldReturn409WhenJobNotInFailedStatus() {
            UUID documentId = UUID.randomUUID();
            testDataBuilder.createQueuedJob(workspaceId, projectId, documentId, "test/key.txt", "text/plain");

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs/" + documentId + "/retry",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(jwt)),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("should return 404 when job doesn't exist")
        void shouldReturn404WhenJobDoesNotExist() {
            UUID nonExistentDocumentId = UUID.randomUUID();

            String jwt = jwtTestUtils.generateInternalServiceJwt();

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/internal/ingestion/jobs/" + nonExistentDocumentId + "/retry",
                    HttpMethod.POST,
                    new HttpEntity<>(authHeaders(jwt)),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
