package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentDownloadUrlResponse;
import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.domain.exception.DocumentAccessForbiddenException;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotUploadedException;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DownloadServiceTest {

    @Mock
    private WorkspaceServiceUserClient workspaceServiceUserClient;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private DownloadService downloadService;

    private UUID documentId;
    private UUID workspaceId;
    private UUID projectId;
    private String storageKey;
    private String mimeType;
    private String documentName;
    private URL mockUrl;

    @BeforeEach
    void setUp() throws MalformedURLException {
        ReflectionTestUtils.setField(downloadService, "bucketName", "test-bucket");

        documentId = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
        workspaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        projectId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
        storageKey = "ws/" + workspaceId + "/projects/" + projectId + "/documents/" + documentId;
        mimeType = "application/pdf";
        documentName = "test-document.pdf";
        mockUrl = new URL("https://s3.example.com/presigned-download-url");
    }

    private DocumentInternalResponse createDocumentResponse(DocumentStatus status) {
        return new DocumentInternalResponse(
                documentId,
                projectId,
                workspaceId,
                documentName,
                status,
                storageKey,
                mimeType,
                1024L,
                UUID.randomUUID(),
                Instant.now()
        );
    }

    @Test
    void getDownloadUrl_validDocument_returnsPresignedUrl() {
        DocumentInternalResponse document = createDocumentResponse(DocumentStatus.UPLOADED);
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(document);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(mockUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        DocumentDownloadUrlResponse result = downloadService.getDownloadUrl(documentId.toString());

        assertNotNull(result);
        assertEquals(mockUrl.toString(), result.url());
        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void getDownloadUrl_documentNotFound_throwsDocumentNotFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, new RequestTemplate());
        FeignException.NotFound notFoundException = new FeignException.NotFound(
                "Not Found", request, null, null);

        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenThrow(notFoundException);

        assertThrows(DocumentNotFoundException.class, () ->
                downloadService.getDownloadUrl(documentId.toString()));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void getDownloadUrl_accessForbidden_throwsDocumentAccessForbiddenException() {
        Request request = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, new RequestTemplate());
        FeignException.Forbidden forbiddenException = new FeignException.Forbidden(
                "Forbidden", request, null, null);

        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenThrow(forbiddenException);

        assertThrows(DocumentAccessForbiddenException.class, () ->
                downloadService.getDownloadUrl(documentId.toString()));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void getDownloadUrl_documentUploading_throwsDocumentNotUploadedException() {
        DocumentInternalResponse document = createDocumentResponse(DocumentStatus.UPLOADING);
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(document);

        assertThrows(DocumentNotUploadedException.class, () ->
                downloadService.getDownloadUrl(documentId.toString()));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void getDownloadUrl_documentFailed_throwsDocumentNotUploadedException() {
        DocumentInternalResponse document = createDocumentResponse(DocumentStatus.FAILED);
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(document);

        assertThrows(DocumentNotUploadedException.class, () ->
                downloadService.getDownloadUrl(documentId.toString()));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void getDownloadUrl_documentUploaded_returnsUrl() {
        DocumentInternalResponse document = createDocumentResponse(DocumentStatus.UPLOADED);
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(document);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(mockUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        DocumentDownloadUrlResponse result = downloadService.getDownloadUrl(documentId.toString());

        assertNotNull(result);
        assertEquals(mockUrl.toString(), result.url());
    }

    @Test
    void getDownloadUrl_documentProcessed_returnsUrl() {
        DocumentInternalResponse document = createDocumentResponse(DocumentStatus.PROCESSED);
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(document);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(mockUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        DocumentDownloadUrlResponse result = downloadService.getDownloadUrl(documentId.toString());

        assertNotNull(result);
        assertEquals(mockUrl.toString(), result.url());
    }

    @Test
    void getDownloadUrl_workspaceServiceError_throwsRuntimeException() {
        Request request = Request.create(Request.HttpMethod.GET, "/test", Collections.emptyMap(), null, new RequestTemplate());
        FeignException.InternalServerError serverError = new FeignException.InternalServerError(
                "Internal Server Error", request, null, null);

        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenThrow(serverError);

        assertThrows(RuntimeException.class, () ->
                downloadService.getDownloadUrl(documentId.toString()));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void getDownloadUrl_presignerFails_throwsRuntimeException() {
        DocumentInternalResponse document = createDocumentResponse(DocumentStatus.UPLOADED);
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(document);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("S3 presigner error"));

        assertThrows(RuntimeException.class, () ->
                downloadService.getDownloadUrl(documentId.toString()));

        verify(workspaceServiceUserClient).getInternalDocument(documentId.toString());
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void getDownloadUrl_setsContentDispositionHeader() {
        DocumentInternalResponse document = createDocumentResponse(DocumentStatus.UPLOADED);
        when(workspaceServiceUserClient.getInternalDocument(documentId.toString()))
                .thenReturn(document);

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(mockUrl);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        when(s3Presigner.presignGetObject(captor.capture())).thenReturn(presignedRequest);

        downloadService.getDownloadUrl(documentId.toString());

        GetObjectPresignRequest capturedRequest = captor.getValue();
        GetObjectRequest getRequest = capturedRequest.getObjectRequest();
        assertEquals("attachment; filename=\"" + documentName + "\"", getRequest.responseContentDisposition());
    }
}
