package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.domain.service.chunker.ChunkerService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private IngestionJobRepository ingestionJobRepository;

    @Mock
    private DocumentFetcher documentFetcher;

    @Mock
    private ExtractionService extractionService;

    @Mock
    private ChunkerService chunkerService;

    @Mock
    private VectorProcessingEventProducer vectorProcessingEventProducer;

    @Mock
    private IngestionJobCacheService ingestionJobCacheService;

    @InjectMocks
    private IngestionService ingestionService;

    @Captor
    private ArgumentCaptor<IngestionJob> jobCaptor;

    @Captor
    private ArgumentCaptor<VectorProcessingRequestEvent> eventCaptor;

    private UUID documentId;
    private UUID workspaceId;
    private UUID projectId;
    private UUID jobId;
    private DocumentUploadedEvent event;
    private IngestionJob job;
    private byte[] documentBytes;
    private ExtractedDocument extractedDocument;
    private List<Chunk> chunks;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        jobId = UUID.randomUUID();

        event = new DocumentUploadedEvent(
                documentId.toString(),
                workspaceId.toString(),
                projectId.toString(),
                "workspaces/" + workspaceId + "/documents/" + documentId + ".pdf",
                "application/pdf",
                1024L
        );

        job = IngestionJob.builder()
                .id(jobId)
                .documentId(documentId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .storageKey(event.storageKey())
                .mimeType(event.mimeType())
                .status(IngestionStatus.QUEUED)
                .stage(IngestionStage.QUEUED)
                .retryCount(0)
                .version(0L)
                .build();

        documentBytes = "Sample PDF content".getBytes();

        extractedDocument = new ExtractedDocument(
                "Extracted text content",
                List.of(new DocumentSection("1", "Section 1", 1, "Section content")),
                Map.of("pages", 1)
        );

        chunks = List.of(
                new Chunk(documentId, 1, 0, 100, "First chunk", 0),
                new Chunk(documentId, 1, 80, 200, "Second chunk", 1)
        );
    }

    @Test
    void process_newDocument_createsJobAndProcesses() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            if (savedJob.getId() == null) {
                savedJob.setId(jobId);
            }
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(extractedDocument);
        when(chunkerService.chunkDocument(extractedDocument, documentId)).thenReturn(chunks);

        ingestionService.process(event);

        verify(ingestionJobRepository).findByDocumentId(documentId);
        verify(ingestionJobRepository, atLeast(1)).save(any(IngestionJob.class));
        verify(documentFetcher).fetch(event.storageKey());
        verify(extractionService).extract(documentBytes, event.mimeType());
        verify(chunkerService).chunkDocument(extractedDocument, documentId);
        verify(vectorProcessingEventProducer).sendVectorProcessingRequestEvent(any(VectorProcessingRequestEvent.class));
    }

    @Test
    void process_existingJob_reusesExistingJob() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(extractedDocument);
        when(chunkerService.chunkDocument(extractedDocument, documentId)).thenReturn(chunks);

        ingestionService.process(event);

        verify(ingestionJobRepository).findByDocumentId(documentId);
        verify(ingestionJobRepository, never()).save(argThat(j -> j.getId() == null));
        verify(documentFetcher).fetch(event.storageKey());
    }

    @Test
    void process_completedJob_skipsProcessing() {
        job.setStatus(IngestionStatus.COMPLETED);
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));

        ingestionService.process(event);

        verify(ingestionJobRepository).findByDocumentId(documentId);
        verify(documentFetcher, never()).fetch(anyString());
        verify(extractionService, never()).extract(any(), anyString());
        verify(chunkerService, never()).chunkDocument(any(), any());
        verify(vectorProcessingEventProducer, never()).sendVectorProcessingRequestEvent(any());
    }

    @Test
    void process_fetchesDocumentFromS3_extractsText() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(extractedDocument);
        when(chunkerService.chunkDocument(extractedDocument, documentId)).thenReturn(chunks);

        ingestionService.process(event);

        verify(documentFetcher).fetch(event.storageKey());
        verify(extractionService).extract(documentBytes, event.mimeType());
    }

    @Test
    void process_chunksContent_sendsVectorProcessingEvent() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(extractedDocument);
        when(chunkerService.chunkDocument(extractedDocument, documentId)).thenReturn(chunks);

        ingestionService.process(event);

        verify(vectorProcessingEventProducer).sendVectorProcessingRequestEvent(eventCaptor.capture());

        VectorProcessingRequestEvent capturedEvent = eventCaptor.getValue();
        assertEquals(documentId.toString(), capturedEvent.documentId());
        assertEquals(projectId.toString(), capturedEvent.projectId());
        assertEquals(workspaceId.toString(), capturedEvent.workspaceId());
        assertEquals(chunks, capturedEvent.chunks());
        assertEquals(jobId.toString(), capturedEvent.ingestionJobId());
    }

    @Test
    void process_fetchFailed_incrementsRetryCountAndThrows() {
        RuntimeException fetchException = new RuntimeException("S3 connection failed");
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenThrow(fetchException);

        assertThrows(RuntimeException.class, () -> ingestionService.process(event));

        verify(ingestionJobRepository, atLeast(1)).save(jobCaptor.capture());

        List<IngestionJob> savedJobs = jobCaptor.getAllValues();
        IngestionJob lastSavedJob = savedJobs.get(savedJobs.size() - 1);
        assertEquals(1, lastSavedJob.getRetryCount());
        assertEquals("S3 connection failed", lastSavedJob.getLastError());
    }

    @Test
    void process_extractionFailed_marksJobFailedAtParseStage() {
        RuntimeException extractionException = new RuntimeException("PDF parsing failed");
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenThrow(extractionException);

        ingestionService.process(event);

        verify(ingestionJobRepository, atLeast(1)).save(jobCaptor.capture());

        List<IngestionJob> savedJobs = jobCaptor.getAllValues();
        boolean hasFailedStatus = savedJobs.stream()
                .anyMatch(j -> j.getStatus() == IngestionStatus.FAILED);
        assertTrue(hasFailedStatus);

        boolean hasParseStage = savedJobs.stream()
                .anyMatch(j -> j.getStage() == IngestionStage.PARSE);
        assertTrue(hasParseStage);
    }

    @Test
    void process_chunkingFailed_marksJobFailedAtChunkStage() {
        RuntimeException chunkingException = new RuntimeException("Chunking failed");
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(extractedDocument);
        when(chunkerService.chunkDocument(extractedDocument, documentId)).thenThrow(chunkingException);

        ingestionService.process(event);

        verify(ingestionJobRepository, atLeast(1)).save(jobCaptor.capture());

        List<IngestionJob> savedJobs = jobCaptor.getAllValues();
        boolean hasFailedStatus = savedJobs.stream()
                .anyMatch(j -> j.getStatus() == IngestionStatus.FAILED);
        assertTrue(hasFailedStatus);

        boolean hasChunkStage = savedJobs.stream()
                .anyMatch(j -> j.getStage() == IngestionStage.CHUNK);
        assertTrue(hasChunkStage);
    }

    @Test
    void process_extractionReturnsNull_stopsProcessing() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(null);

        ingestionService.process(event);

        verify(chunkerService, never()).chunkDocument(any(), any());
        verify(vectorProcessingEventProducer, never()).sendVectorProcessingRequestEvent(any());
    }

    @Test
    void process_chunkingReturnsNull_stopsProcessing() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(extractedDocument);
        when(chunkerService.chunkDocument(extractedDocument, documentId)).thenReturn(null);

        ingestionService.process(event);

        verify(vectorProcessingEventProducer, never()).sendVectorProcessingRequestEvent(any());
    }

    @Test
    void process_successfulCompletion_updatesStageToEmbed() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(extractedDocument);
        when(chunkerService.chunkDocument(extractedDocument, documentId)).thenReturn(chunks);

        ingestionService.process(event);

        verify(ingestionJobRepository, atLeast(1)).save(jobCaptor.capture());

        List<IngestionJob> savedJobs = jobCaptor.getAllValues();
        IngestionJob lastSavedJob = savedJobs.get(savedJobs.size() - 1);
        assertEquals(IngestionStage.EMBED, lastSavedJob.getStage());
    }

    @Test
    void process_setsStatusToProcessingAtStart() {
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(extractedDocument);
        when(chunkerService.chunkDocument(extractedDocument, documentId)).thenReturn(chunks);

        ingestionService.process(event);

        verify(ingestionJobRepository, atLeast(1)).save(jobCaptor.capture());

        List<IngestionJob> savedJobs = jobCaptor.getAllValues();
        boolean hasProcessingStatus = savedJobs.stream()
                .anyMatch(j -> j.getStatus() == IngestionStatus.PROCESSING);
        assertTrue(hasProcessingStatus);
    }

    @Test
    void process_emptyChunks_sendsEventWithEmptyList() {
        List<Chunk> emptyChunks = List.of();
        when(ingestionJobRepository.findByDocumentId(documentId)).thenReturn(Optional.of(job));
        when(ingestionJobRepository.save(any(IngestionJob.class))).thenAnswer(invocation -> {
            IngestionJob savedJob = invocation.getArgument(0);
            savedJob.setVersion(savedJob.getVersion() != null ? savedJob.getVersion() + 1 : 1L);
            return savedJob;
        });
        when(documentFetcher.fetch(event.storageKey())).thenReturn(documentBytes);
        when(extractionService.extract(documentBytes, event.mimeType())).thenReturn(extractedDocument);
        when(chunkerService.chunkDocument(extractedDocument, documentId)).thenReturn(emptyChunks);

        ingestionService.process(event);

        verify(vectorProcessingEventProducer).sendVectorProcessingRequestEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().chunks().isEmpty());
    }
}
