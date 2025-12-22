package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentIngestionCompletedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionStatusOrchestratorTest {

    @Mock
    private IngestionJobRepository ingestionJobRepository;

    @Mock
    private DocumentIngestionCompletedEventProducer documentIngestionCompletedEventProducer;

    @InjectMocks
    private IngestionStatusOrchestrator orchestrator;

    @Captor
    private ArgumentCaptor<IngestionJob> jobCaptor;

    @Captor
    private ArgumentCaptor<DocumentIngestionCompletedEvent> eventCaptor;

    private UUID jobId;
    private UUID documentId;
    private UUID workspaceId;
    private UUID projectId;
    private IngestionJob job;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        job = IngestionJob.builder()
                .id(jobId)
                .documentId(documentId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .storageKey("workspaces/" + workspaceId + "/documents/" + documentId + ".pdf")
                .mimeType("application/pdf")
                .status(IngestionStatus.PROCESSING)
                .stage(IngestionStage.EMBED)
                .retryCount(0)
                .version(0L)
                .build();
    }

    @Test
    void handleVectorProcessingCompleted_existingJob_marksCompleted() {
        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.handleVectorProcessingCompleted(jobId.toString());

        verify(ingestionJobRepository).save(jobCaptor.capture());
        IngestionJob savedJob = jobCaptor.getValue();
        assertEquals(IngestionStatus.COMPLETED, savedJob.getStatus());
    }

    @Test
    void handleVectorProcessingCompleted_publishesCompletionEvent() {
        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.handleVectorProcessingCompleted(jobId.toString());

        verify(documentIngestionCompletedEventProducer).sendDocumentIngestionCompletedEvent(eventCaptor.capture());
        DocumentIngestionCompletedEvent event = eventCaptor.getValue();
        assertEquals(documentId.toString(), event.documentId());
        assertEquals(workspaceId.toString(), event.workspaceId());
        assertEquals(projectId.toString(), event.projectId());
    }

    @Test
    void handleVectorProcessingCompleted_nonExistingJob_doesNotThrow() {
        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> orchestrator.handleVectorProcessingCompleted(jobId.toString()));

        verify(ingestionJobRepository, never()).save(any());
        verify(documentIngestionCompletedEventProducer, never()).sendDocumentIngestionCompletedEvent(any());
    }

    @Test
    void handleVectorProcessingCompleted_savesJobAfterPublishingEvent() {
        when(ingestionJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.handleVectorProcessingCompleted(jobId.toString());

        var inOrder = inOrder(documentIngestionCompletedEventProducer, ingestionJobRepository);
        inOrder.verify(documentIngestionCompletedEventProducer).sendDocumentIngestionCompletedEvent(any());
        inOrder.verify(ingestionJobRepository).save(any());
    }
}
