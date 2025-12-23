package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmbeddingRequestEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingCompletedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.EmbeddingJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingRequestEventProducer embeddingRequestEventProducer;

    @Mock
    private EmbeddingJobRepository jobRepository;

    @Mock
    private VectorStorageService vectorStorageService;

    @Mock
    private VectorProcessingCompletedEventProducer vectorProcessingCompletedEventProducer;

    @InjectMocks
    private EmbeddingService embeddingService;

    private UUID workspaceId;
    private UUID projectId;
    private UUID documentId;
    private UUID ingestionJobId;
    private UUID embeddingJobId;
    private VectorProcessingRequestEvent vectorProcessingRequestEvent;
    private EmbeddingJob savedJob;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        ingestionJobId = UUID.randomUUID();
        embeddingJobId = UUID.randomUUID();

        savedJob = EmbeddingJob.builder()
                .id(embeddingJobId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .ingestionJobId(ingestionJobId)
                .status(EmbeddingStatus.IN_PROGRESS)
                .totalChunks(5)
                .processedChunks(0)
                .build();
    }

    private List<Chunk> createChunks(int count) {
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(new Chunk(documentId, i / 2, i * 100, (i + 1) * 100, "Content " + i, i));
        }
        return chunks;
    }

    private List<EmbeddedChunk> createEmbeddedChunks(int count) {
        List<EmbeddedChunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(new EmbeddedChunk(
                    documentId,
                    i / 2,
                    i * 100,
                    (i + 1) * 100,
                    List.of(0.1f, 0.2f, 0.3f),
                    "Content " + i,
                    i
            ));
        }
        return chunks;
    }

    @Test
    void process_validRequest_createsJobWithCorrectFields() {
        List<Chunk> chunks = createChunks(5);
        vectorProcessingRequestEvent = new VectorProcessingRequestEvent(
                ingestionJobId.toString(),
                documentId.toString(),
                projectId.toString(),
                workspaceId.toString(),
                chunks
        );

        when(jobRepository.save(any(EmbeddingJob.class))).thenReturn(savedJob);

        embeddingService.process(vectorProcessingRequestEvent);

        ArgumentCaptor<EmbeddingJob> jobCaptor = ArgumentCaptor.forClass(EmbeddingJob.class);
        verify(jobRepository).save(jobCaptor.capture());

        EmbeddingJob capturedJob = jobCaptor.getValue();
        assertEquals(ingestionJobId, capturedJob.getIngestionJobId());
        assertEquals(documentId, capturedJob.getDocumentId());
        assertEquals(projectId, capturedJob.getProjectId());
        assertEquals(workspaceId, capturedJob.getWorkspaceId());
        assertEquals(5, capturedJob.getTotalChunks());
        assertEquals(0, capturedJob.getProcessedChunks());
        assertEquals(EmbeddingStatus.IN_PROGRESS, capturedJob.getStatus());
    }

    @Test
    void process_fiveChunks_createsSingleBatch() {
        List<Chunk> chunks = createChunks(5);
        vectorProcessingRequestEvent = new VectorProcessingRequestEvent(
                ingestionJobId.toString(),
                documentId.toString(),
                projectId.toString(),
                workspaceId.toString(),
                chunks
        );

        when(jobRepository.save(any(EmbeddingJob.class))).thenReturn(savedJob);

        embeddingService.process(vectorProcessingRequestEvent);

        verify(embeddingRequestEventProducer, times(1)).sendEmbeddingRequestEvent(any(EmbeddingRequestEvent.class));
    }

    @Test
    void process_twentyFiveChunks_createsThreeBatches() {
        List<Chunk> chunks = createChunks(25);
        vectorProcessingRequestEvent = new VectorProcessingRequestEvent(
                ingestionJobId.toString(),
                documentId.toString(),
                projectId.toString(),
                workspaceId.toString(),
                chunks
        );

        EmbeddingJob jobWith25Chunks = EmbeddingJob.builder()
                .id(embeddingJobId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .ingestionJobId(ingestionJobId)
                .status(EmbeddingStatus.IN_PROGRESS)
                .totalChunks(25)
                .processedChunks(0)
                .build();

        when(jobRepository.save(any(EmbeddingJob.class))).thenReturn(jobWith25Chunks);

        embeddingService.process(vectorProcessingRequestEvent);

        verify(embeddingRequestEventProducer, times(3)).sendEmbeddingRequestEvent(any(EmbeddingRequestEvent.class));
    }

    @Test
    void process_setsCorrectModelInRequest() {
        List<Chunk> chunks = createChunks(5);
        vectorProcessingRequestEvent = new VectorProcessingRequestEvent(
                ingestionJobId.toString(),
                documentId.toString(),
                projectId.toString(),
                workspaceId.toString(),
                chunks
        );

        when(jobRepository.save(any(EmbeddingJob.class))).thenReturn(savedJob);

        embeddingService.process(vectorProcessingRequestEvent);

        ArgumentCaptor<EmbeddingRequestEvent> eventCaptor = ArgumentCaptor.forClass(EmbeddingRequestEvent.class);
        verify(embeddingRequestEventProducer).sendEmbeddingRequestEvent(eventCaptor.capture());

        EmbeddingRequestEvent capturedEvent = eventCaptor.getValue();
        assertEquals("text-embedding-3-small", capturedEvent.model());
        assertEquals(embeddingJobId.toString(), capturedEvent.embeddingJobId());
        assertEquals(5, capturedEvent.chunks().size());
    }

    @Test
    void processEmbeddingCompletedEvent_existingJob_upsertsVectors() {
        List<EmbeddedChunk> embeddedChunks = createEmbeddedChunks(5);
        EmbeddingCompletedEvent event = new EmbeddingCompletedEvent(
                embeddingJobId.toString(),
                embeddedChunks,
                "text-embedding-3-small"
        );

        when(jobRepository.findById(embeddingJobId)).thenReturn(Optional.of(savedJob));

        embeddingService.processEmbeddingCompletedEvent(event);

        ArgumentCaptor<List<EmbeddedChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<ChunkMetadata> metadataCaptor = ArgumentCaptor.forClass(ChunkMetadata.class);
        verify(vectorStorageService).upsert(chunksCaptor.capture(), metadataCaptor.capture());

        assertEquals(5, chunksCaptor.getValue().size());
        assertEquals(workspaceId, metadataCaptor.getValue().workspaceId());
        assertEquals(projectId, metadataCaptor.getValue().projectId());
    }

    @Test
    void processEmbeddingCompletedEvent_tracksProgress() {
        List<EmbeddedChunk> embeddedChunks = createEmbeddedChunks(3);
        EmbeddingCompletedEvent event = new EmbeddingCompletedEvent(
                embeddingJobId.toString(),
                embeddedChunks,
                "text-embedding-3-small"
        );

        EmbeddingJob jobWithTenChunks = EmbeddingJob.builder()
                .id(embeddingJobId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .ingestionJobId(ingestionJobId)
                .status(EmbeddingStatus.IN_PROGRESS)
                .totalChunks(10)
                .processedChunks(0)
                .build();

        when(jobRepository.findById(embeddingJobId)).thenReturn(Optional.of(jobWithTenChunks));

        embeddingService.processEmbeddingCompletedEvent(event);

        ArgumentCaptor<EmbeddingJob> jobCaptor = ArgumentCaptor.forClass(EmbeddingJob.class);
        verify(jobRepository).save(jobCaptor.capture());

        assertEquals(3, jobCaptor.getValue().getProcessedChunks());
        assertEquals(EmbeddingStatus.IN_PROGRESS, jobCaptor.getValue().getStatus());
    }

    @Test
    void processEmbeddingCompletedEvent_partialCompletion_statusStaysInProgress() {
        List<EmbeddedChunk> embeddedChunks = createEmbeddedChunks(5);
        EmbeddingCompletedEvent event = new EmbeddingCompletedEvent(
                embeddingJobId.toString(),
                embeddedChunks,
                "text-embedding-3-small"
        );

        EmbeddingJob jobWithTenChunks = EmbeddingJob.builder()
                .id(embeddingJobId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .ingestionJobId(ingestionJobId)
                .status(EmbeddingStatus.IN_PROGRESS)
                .totalChunks(10)
                .processedChunks(0)
                .build();

        when(jobRepository.findById(embeddingJobId)).thenReturn(Optional.of(jobWithTenChunks));

        embeddingService.processEmbeddingCompletedEvent(event);

        ArgumentCaptor<EmbeddingJob> jobCaptor = ArgumentCaptor.forClass(EmbeddingJob.class);
        verify(jobRepository).save(jobCaptor.capture());

        assertEquals(EmbeddingStatus.IN_PROGRESS, jobCaptor.getValue().getStatus());
        verify(vectorProcessingCompletedEventProducer, never()).sendVectorProcessingCompletedEvent(any());
    }

    @Test
    void processEmbeddingCompletedEvent_allChunksComplete_marksJobComplete() {
        List<EmbeddedChunk> embeddedChunks = createEmbeddedChunks(5);
        EmbeddingCompletedEvent event = new EmbeddingCompletedEvent(
                embeddingJobId.toString(),
                embeddedChunks,
                "text-embedding-3-small"
        );

        when(jobRepository.findById(embeddingJobId)).thenReturn(Optional.of(savedJob));

        embeddingService.processEmbeddingCompletedEvent(event);

        ArgumentCaptor<EmbeddingJob> jobCaptor = ArgumentCaptor.forClass(EmbeddingJob.class);
        verify(jobRepository).save(jobCaptor.capture());

        assertEquals(EmbeddingStatus.COMPLETED, jobCaptor.getValue().getStatus());
        assertEquals("text-embedding-3-small", jobCaptor.getValue().getModelUsed());
        assertEquals(5, jobCaptor.getValue().getProcessedChunks());
    }

    @Test
    void processEmbeddingCompletedEvent_completed_publishesCompletionEvent() {
        List<EmbeddedChunk> embeddedChunks = createEmbeddedChunks(5);
        EmbeddingCompletedEvent event = new EmbeddingCompletedEvent(
                embeddingJobId.toString(),
                embeddedChunks,
                "text-embedding-3-small"
        );

        when(jobRepository.findById(embeddingJobId)).thenReturn(Optional.of(savedJob));

        embeddingService.processEmbeddingCompletedEvent(event);

        ArgumentCaptor<VectorProcessingCompletedEvent> eventCaptor = ArgumentCaptor.forClass(VectorProcessingCompletedEvent.class);
        verify(vectorProcessingCompletedEventProducer).sendVectorProcessingCompletedEvent(eventCaptor.capture());

        VectorProcessingCompletedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(ingestionJobId.toString(), capturedEvent.ingestionJobId());
        assertEquals(documentId.toString(), capturedEvent.documentId());
        assertEquals(projectId.toString(), capturedEvent.projectId());
        assertEquals(workspaceId.toString(), capturedEvent.workspaceId());
    }

    @Test
    void processEmbeddingCompletedEvent_jobNotFound_throwsException() {
        List<EmbeddedChunk> embeddedChunks = createEmbeddedChunks(5);
        EmbeddingCompletedEvent event = new EmbeddingCompletedEvent(
                embeddingJobId.toString(),
                embeddedChunks,
                "text-embedding-3-small"
        );

        when(jobRepository.findById(embeddingJobId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> embeddingService.processEmbeddingCompletedEvent(event));

        assertEquals("Job not found", exception.getMessage());
        verify(vectorStorageService, never()).upsert(any(), any());
        verify(jobRepository, never()).save(any());
    }
}
