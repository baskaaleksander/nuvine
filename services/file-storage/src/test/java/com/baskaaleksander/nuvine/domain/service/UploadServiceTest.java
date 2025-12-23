package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.application.dto.UploadUrlResponse;
import com.baskaaleksander.nuvine.domain.exception.DocumentAccessForbiddenException;
import com.baskaaleksander.nuvine.domain.exception.DocumentConflictException;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceUserClient;
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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private WorkspaceServiceUserClient workspaceServiceUserClient;

    @InjectMocks
    private UploadService uploadService;

    private UUID documentId;
    private UUID workspaceId;
    private UUID projectId;
    private String contentType;
    private Long sizeBytes;
    private DocumentInternalResponse documentInternalResponse;
    private URL mockUrl;

    @BeforeEach
    void setUp() throws MalformedURLException {
        ReflectionTestUtils.setField(uploadService, "bucket", "test-bucket");

        documentId = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
        workspaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        projectId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
        contentType = "application/pdf";
        sizeBytes = 1024L;
        mockUrl = new URL("https://s3.example.com/presigned-url");

        documentInternalResponse = new DocumentInternalResponse(
                documentId,
                projectId,
                workspaceId,
                "test-document.pdf",
                DocumentStatus.UPLOADING,
                null,
                contentType,
                sizeBytes,
                UUID.randomUUID(),
                Instant.now()
        );
    }

    @Test
    void generatePresignedUploadUrl_validDocument_returnsPresignedUrl() {
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(documentInternalResponse);

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(mockUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        UploadUrlResponse result = uploadService.generatePresignedUploadUrl(
                documentId.toString(), contentType, sizeBytes);

        assertNotNull(result);
        assertEquals(mockUrl, result.url());
        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void generatePresignedUploadUrl_documentNotFound_throwsDocumentNotFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, new RequestTemplate());
        FeignException.NotFound notFoundException = new FeignException.NotFound(
                "Not Found", request, null, null);

        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenThrow(notFoundException);

        assertThrows(DocumentNotFoundException.class, () ->
                uploadService.generatePresignedUploadUrl(documentId.toString(), contentType, sizeBytes));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void generatePresignedUploadUrl_accessForbidden_throwsDocumentAccessForbiddenException() {
        Request request = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, new RequestTemplate());
        FeignException.Forbidden forbiddenException = new FeignException.Forbidden(
                "Forbidden", request, null, null);

        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenThrow(forbiddenException);

        assertThrows(DocumentAccessForbiddenException.class, () ->
                uploadService.generatePresignedUploadUrl(documentId.toString(), contentType, sizeBytes));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void generatePresignedUploadUrl_wrongStatus_throwsDocumentConflictException() {
        DocumentInternalResponse uploadedDocument = new DocumentInternalResponse(
                documentId,
                projectId,
                workspaceId,
                "test-document.pdf",
                DocumentStatus.UPLOADED,
                "ws/123/projects/456/documents/789",
                contentType,
                sizeBytes,
                UUID.randomUUID(),
                Instant.now()
        );

        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(uploadedDocument);

        assertThrows(DocumentConflictException.class, () ->
                uploadService.generatePresignedUploadUrl(documentId.toString(), contentType, sizeBytes));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void generatePresignedUploadUrl_workspaceServiceError_throwsRuntimeException() {
        Request request = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, new RequestTemplate());
        FeignException.InternalServerError serverError = new FeignException.InternalServerError(
                "Internal Server Error", request, null, null);

        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenThrow(serverError);

        assertThrows(RuntimeException.class, () ->
                uploadService.generatePresignedUploadUrl(documentId.toString(), contentType, sizeBytes));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void generatePresignedUploadUrl_presignerFails_throwsRuntimeException() {
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(documentInternalResponse);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("S3 presigner error"));

        assertThrows(RuntimeException.class, () ->
                uploadService.generatePresignedUploadUrl(documentId.toString(), contentType, sizeBytes));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verify(s3Presigner).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    void generatePresignedUploadUrl_setsCorrectContentType() {
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(documentInternalResponse);

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(mockUrl);

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        when(s3Presigner.presignPutObject(captor.capture())).thenReturn(presignedRequest);

        uploadService.generatePresignedUploadUrl(documentId.toString(), contentType, sizeBytes);

        PutObjectPresignRequest capturedRequest = captor.getValue();
        PutObjectRequest putRequest = capturedRequest.putObjectRequest();
        assertEquals(contentType, putRequest.contentType());
    }

    @Test
    void generatePresignedUploadUrl_setsCorrectContentLength() {
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(documentInternalResponse);

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(mockUrl);

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        when(s3Presigner.presignPutObject(captor.capture())).thenReturn(presignedRequest);

        uploadService.generatePresignedUploadUrl(documentId.toString(), contentType, sizeBytes);

        PutObjectPresignRequest capturedRequest = captor.getValue();
        PutObjectRequest putRequest = capturedRequest.putObjectRequest();
        assertEquals(sizeBytes, putRequest.contentLength());
    }

    @Test
    void generatePresignedUploadUrl_generatesCorrectStorageKey() {
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(documentInternalResponse);

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(mockUrl);

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        when(s3Presigner.presignPutObject(captor.capture())).thenReturn(presignedRequest);

        String expectedKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;

        uploadService.generatePresignedUploadUrl(documentId.toString(), contentType, sizeBytes);

        PutObjectPresignRequest capturedRequest = captor.getValue();
        PutObjectRequest putRequest = capturedRequest.putObjectRequest();
        assertEquals(expectedKey, putRequest.key());
    }

    @Test
    void generatePresignedUploadUrl_returnsCorrectHttpMethod() {
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(documentInternalResponse);

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(mockUrl);
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        UploadUrlResponse result = uploadService.generatePresignedUploadUrl(
                documentId.toString(), contentType, sizeBytes);

        assertEquals("PUT", result.method());
    }
}
