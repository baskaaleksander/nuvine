package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.IngestionJobConflictException;
import com.baskaaleksander.nuvine.domain.exception.UnauthorizedDocumentAccessException;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentUploadedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionCommandServiceTest {

    @Mock
    private WorkspaceServiceClient workspaceServiceClient;

    @Mock
    private IngestionService ingestionService;

    @Mock
    private DocumentUploadedEventProducer producer;

    @Mock
    private IngestionJobRepository ingestionJobRepository;

    @Mock
    private IngestionJobCacheService ingestionJobCacheService;

    @InjectMocks
    private IngestionCommandService ingestionCommandService;

    @Captor
    private ArgumentCaptor<DocumentUploadedEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<IngestionJob> jobCaptor;

    private UUID documentId;
    private UUID workspaceId;
    private UUID projectId;
    private DocumentInternalResponse document;
    private IngestionJob failedJob;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        document = new DocumentInternalResponse(
                documentId,
                projectId,
                workspaceId,
                "test-document.pdf",
                DocumentStatus.UPLOADED,
                "workspaces/" + workspaceId + "/documents/" + documentId + ".pdf",
                "application/pdf",
                1024L,
                UUID.randomUUID(),
                Instant.now()
        );

        failedJob = IngestionJob.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .storageKey(document.storageKey())
                .mimeType(document.mimeType())
                .status(IngestionStatus.FAILED)
                .stage(IngestionStage.FETCH)
                .retryCount(1)
                .lastError("Previous error")
                .version(0L)
                .build();
    }

    @Test
    void startIngestionJob_validDocument_publishesDocumentUploadedEvent() {
        when(workspaceServiceClient.getInternalDocument(documentId.toString())).thenReturn(document);

        ingestionCommandService.startIngestionJob(documentId.toString());

        verify(producer).sendDocumentUploadedEvent(eventCaptor.capture());
        DocumentUploadedEvent event = eventCaptor.getValue();
        assertEquals(documentId.toString(), event.documentId());
        assertEquals(workspaceId.toString(), event.workspaceId());
        assertEquals(projectId.toString(), event.projectId());
        assertEquals(document.storageKey(), event.storageKey());
        assertEquals(document.mimeType(), event.mimeType());
    }

    @Test
    void startIngestionJob_documentNotFound_throwsDocumentNotFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/documents/" + documentId,
                Collections.emptyMap(), null, new RequestTemplate());
        FeignException.NotFound notFound = new FeignException.NotFound("Not found", request, null, null);
        when(workspaceServiceClient.getInternalDocument(documentId.toString())).thenThrow(notFound);

        assertThrows(DocumentNotFoundException.class,
                () -> ingestionCommandService.startIngestionJob(documentId.toString()));
        verify(producer, never()).sendDocumentUploadedEvent(any());
    }

    @Test
    void startIngestionJob_unauthorized_throwsUnauthorizedDocumentAccessException() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/documents/" + documentId,
                Collections.emptyMap(), null, new RequestTemplate());
        FeignException.Unauthorized unauthorized = new FeignException.Unauthorized("Unauthorized", request, null, null);
        when(workspaceServiceClient.getInternalDocument(documentId.toString())).thenThrow(unauthorized);

        assertThrows(UnauthorizedDocumentAccessException.class,
                () -> ingestionCommandService.startIngestionJob(documentId.toString()));
    }

    @Test
    void startIngestionJob_forbidden_throwsUnauthorizedDocumentAccessException() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/documents/" + documentId,
                Collections.emptyMap(), null, new RequestTemplate());
        FeignException.Forbidden forbidden = new FeignException.Forbidden("Forbidden", request, null, null);
        when(workspaceServiceClient.getInternalDocument(documentId.toString())).thenThrow(forbidden);

        assertThrows(UnauthorizedDocumentAccessException.class,
                () -> ingestionCommandService.startIngestionJob(documentId.toString()));
    }

    @Test
    void startIngestionJob_otherFeignError_throwsRuntimeException() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/documents/" + documentId,
                Collections.emptyMap(), null, new RequestTemplate());
        FeignException.InternalServerError serverError = new FeignException.InternalServerError("Server error", request, null, null);
        when(workspaceServiceClient.getInternalDocument(documentId.toString())).thenThrow(serverError);

        assertThrows(RuntimeException.class,
                () -> ingestionCommandService.startIngestionJob(documentId.toString()));
    }

    @Test
    void retryIngestionJob_failedJob_resetsAndRestartsJob() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(failedJob));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workspaceServiceClient.getInternalDocument(documentId.toString())).thenReturn(document);

        ingestionCommandService.retryIngestionJob(documentId.toString());

        verify(ingestionJobRepository).save(jobCaptor.capture());
        IngestionJob savedJob = jobCaptor.getValue();
        assertEquals(IngestionStatus.QUEUED, savedJob.getStatus());
        assertEquals(IngestionStage.QUEUED, savedJob.getStage());
        assertEquals(2, savedJob.getRetryCount()); // incremented from 1
        assertNull(savedJob.getLastError());

        verify(producer).sendDocumentUploadedEvent(any());
    }

    @Test
    void retryIngestionJob_nonFailedJob_throwsIngestionJobConflictException() {
        failedJob.setStatus(IngestionStatus.PROCESSING);
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(failedJob));

        assertThrows(IngestionJobConflictException.class,
                () -> ingestionCommandService.retryIngestionJob(documentId.toString()));

        verify(ingestionJobRepository, never()).save(any());
        verify(producer, never()).sendDocumentUploadedEvent(any());
    }

    @Test
    void retryIngestionJob_completedJob_throwsIngestionJobConflictException() {
        failedJob.setStatus(IngestionStatus.COMPLETED);
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(failedJob));

        assertThrows(IngestionJobConflictException.class,
                () -> ingestionCommandService.retryIngestionJob(documentId.toString()));
    }

    @Test
    void retryIngestionJob_jobNotFound_throwsDocumentNotFoundException() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class,
                () -> ingestionCommandService.retryIngestionJob(documentId.toString()));
    }
}
