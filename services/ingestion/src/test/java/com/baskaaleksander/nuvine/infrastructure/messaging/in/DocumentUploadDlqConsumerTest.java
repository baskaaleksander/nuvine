package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.exception.IngestionJobNotFoundException;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUploadDlqConsumerTest {

    @Mock
    private IngestionJobRepository jobRepository;

    @InjectMocks
    private DocumentUploadDlqConsumer consumer;

    @Captor
    private ArgumentCaptor<IngestionJob> jobCaptor;

    private DocumentUploadedEvent event;
    private UUID documentId;
    private IngestionJob job;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        event = new DocumentUploadedEvent(
                documentId.toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "workspaces/test/documents/test.pdf",
                "application/pdf",
                1024L
        );

        job = IngestionJob.builder()
                .id(UUID.randomUUID())
                .documentId(documentId)
                .workspaceId(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .storageKey("workspaces/test/documents/test.pdf")
                .mimeType("application/pdf")
                .status(IngestionStatus.PROCESSING)
                .stage(IngestionStage.FETCH)
                .retryCount(0)
                .version(0L)
                .build();
    }

    @Test
    void consumeDocumentUploadDlq_existingJob_updatesStatusToFailed() {
        when(jobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(IngestionJob.class))).thenReturn(job);

        consumer.consumeDocumentUploadDlq(
                event,
                "document-uploaded",
                0,
                100L,
                3
        );

        verify(jobRepository).save(jobCaptor.capture());
        
        IngestionJob savedJob = jobCaptor.getValue();
        assertEquals(IngestionStatus.FAILED, savedJob.getStatus());
        assertTrue(savedJob.getLastError().contains("FAILED_AFTER_MAX_RETRIES"));
        assertTrue(savedJob.getLastError().contains("attempts=3"));
    }

    @Test
    void consumeDocumentUploadDlq_nonExistingJob_throwsException() {
        when(jobRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        assertThrows(IngestionJobNotFoundException.class, () ->
                consumer.consumeDocumentUploadDlq(
                        event,
                        "document-uploaded",
                        0,
                        100L,
                        3
                )
        );

        verify(jobRepository, never()).save(any());
    }
}
