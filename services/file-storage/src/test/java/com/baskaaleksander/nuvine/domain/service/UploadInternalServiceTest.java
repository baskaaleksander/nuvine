package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.UploadCompletedRequest;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadInternalServiceTest {

    @Mock
    private WorkspaceServiceServiceClient workspaceServiceServiceClient;

    @InjectMocks
    private UploadInternalService uploadInternalService;

    private ObjectMapper objectMapper;
    private String validSecret;
    private String validAuthHeader;

    private UUID workspaceId;
    private UUID projectId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validSecret = "test-webhook-secret";
        validAuthHeader = "Bearer " + validSecret;

        ReflectionTestUtils.setField(uploadInternalService, "webhookSecret", validSecret);

        workspaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        projectId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
        documentId = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
    }

    private JsonNode loadJsonResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            return objectMapper.readTree(is);
        }
    }

    @Test
    void handleMinioEvent_validSecret_processesEvent() throws IOException {
        JsonNode body = loadJsonResource("minio-webhook-event.json");

        uploadInternalService.handleMinioEvent(body, validAuthHeader);

        verify(workspaceServiceServiceClient).uploadCompleted(eq(documentId), any(UploadCompletedRequest.class));
    }

    @Test
    void handleMinioEvent_invalidSecret_returnsEarlyWithoutProcessing() throws IOException {
        JsonNode body = loadJsonResource("minio-webhook-event.json");
        String invalidAuthHeader = "Bearer wrong-secret";

        uploadInternalService.handleMinioEvent(body, invalidAuthHeader);

        verifyNoInteractions(workspaceServiceServiceClient);
    }

    @Test
    void handleMinioEvent_nullAuthHeader_returnsEarlyWithoutProcessing() throws IOException {
        JsonNode body = loadJsonResource("minio-webhook-event.json");

        uploadInternalService.handleMinioEvent(body, null);

        verifyNoInteractions(workspaceServiceServiceClient);
    }

    @Test
    void handleMinioEvent_parsesStorageKey_notifiesWorkspace() throws IOException {
        JsonNode body = loadJsonResource("minio-webhook-event.json");
        ArgumentCaptor<UUID> documentIdCaptor = ArgumentCaptor.forClass(UUID.class);

        uploadInternalService.handleMinioEvent(body, validAuthHeader);

        verify(workspaceServiceServiceClient).uploadCompleted(documentIdCaptor.capture(), any(UploadCompletedRequest.class));
        assertEquals(documentId, documentIdCaptor.getValue());
    }

    @Test
    void handleMinioEvent_invalidKeyFormat_logsErrorAndReturns() throws Exception {
        String invalidKeyJson = """
                {
                  "Records": [
                    {
                      "s3": {
                        "bucket": {
                          "name": "nuvine-documents"
                        },
                        "object": {
                          "key": "invalid-key-format",
                          "size": 1024,
                          "contentType": "application/pdf"
                        }
                      }
                    }
                  ]
                }
                """;
        JsonNode body = objectMapper.readTree(invalidKeyJson);

        uploadInternalService.handleMinioEvent(body, validAuthHeader);

        verifyNoInteractions(workspaceServiceServiceClient);
    }

    @Test
    void handleMinioEvent_workspaceServiceError_logsErrorAndContinues() throws IOException {
        JsonNode body = loadJsonResource("minio-webhook-event.json");
        Request request = Request.create(Request.HttpMethod.PATCH, "/test", Collections.emptyMap(), null, new RequestTemplate());
        FeignException.InternalServerError serverError = new FeignException.InternalServerError(
                "Internal Server Error", request, null, null);

        doThrow(serverError).when(workspaceServiceServiceClient)
                .uploadCompleted(any(UUID.class), any(UploadCompletedRequest.class));

        assertDoesNotThrow(() -> uploadInternalService.handleMinioEvent(body, validAuthHeader));

        verify(workspaceServiceServiceClient).uploadCompleted(eq(documentId), any(UploadCompletedRequest.class));
    }

    @Test
    void handleMinioEvent_decodesUrlEncodedKey() throws IOException {
        JsonNode body = loadJsonResource("minio-webhook-event-url-encoded.json");
        ArgumentCaptor<UUID> documentIdCaptor = ArgumentCaptor.forClass(UUID.class);

        uploadInternalService.handleMinioEvent(body, validAuthHeader);

        verify(workspaceServiceServiceClient).uploadCompleted(documentIdCaptor.capture(), any(UploadCompletedRequest.class));
        assertEquals(documentId, documentIdCaptor.getValue());
    }

    @Test
    void handleMinioEvent_extractsCorrectMetadata() throws IOException {
        JsonNode body = loadJsonResource("minio-webhook-event.json");
        ArgumentCaptor<UploadCompletedRequest> requestCaptor = ArgumentCaptor.forClass(UploadCompletedRequest.class);

        uploadInternalService.handleMinioEvent(body, validAuthHeader);

        verify(workspaceServiceServiceClient).uploadCompleted(any(UUID.class), requestCaptor.capture());
        UploadCompletedRequest capturedRequest = requestCaptor.getValue();

        assertEquals(1024L, capturedRequest.sizeBytes());
        assertEquals("application/pdf", capturedRequest.mimeType());
    }

    @Test
    void handleMinioEvent_sendsCorrectUploadCompletedRequest() throws IOException {
        JsonNode body = loadJsonResource("minio-webhook-event.json");
        String expectedStorageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;
        ArgumentCaptor<UploadCompletedRequest> requestCaptor = ArgumentCaptor.forClass(UploadCompletedRequest.class);

        uploadInternalService.handleMinioEvent(body, validAuthHeader);

        verify(workspaceServiceServiceClient).uploadCompleted(any(UUID.class), requestCaptor.capture());
        UploadCompletedRequest capturedRequest = requestCaptor.getValue();

        assertEquals(expectedStorageKey, capturedRequest.storageKey());
        assertEquals("application/pdf", capturedRequest.mimeType());
        assertEquals(1024L, capturedRequest.sizeBytes());
    }
}
