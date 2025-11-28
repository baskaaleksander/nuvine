package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.domain.model.ChunkMetadata;
import com.baskaaleksander.nuvine.domain.model.EmbeddingJob;
import com.baskaaleksander.nuvine.domain.model.EmbeddingStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmbeddingRequestEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.EmbeddingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingRequestEventProducer embeddingRequestEventProducer;
    private final EmbeddingJobRepository jobRepository;
    private final VectorStorageService vectorStorageService;

    public void process(VectorProcessingRequestEvent event) {
        int totalChunks = event.chunks().size();

        log.info("EMBEDDING_SERVICE PROCESS START projectId={} documentId={} totalChunks={}", event.projectId(), event.documentId(), totalChunks);

        EmbeddingJob job = EmbeddingJob.builder()
                .ingestionJobId(UUID.fromString(event.ingestionJobId()))
                .documentId(UUID.fromString(event.documentId()))
                .projectId(UUID.fromString(event.projectId()))
                .workspaceId(UUID.fromString(event.workspaceId()))
                .totalChunks(totalChunks)
                .processedChunks(0)
                .status(EmbeddingStatus.IN_PROGRESS)
                .build();

        job = jobRepository.save(job);

        log.info("EMBEDDING_SERVICE PROCESS SAVED jobId={} projectId={} documentId={} totalChunks={}", job.getId(), event.projectId(), event.documentId(), totalChunks);

        List<List<Chunk>> partitionedChunks = partition(event.chunks(), 10);

        for (var batch : partitionedChunks) {
            EmbeddingRequestEvent batchEvent = new EmbeddingRequestEvent(
                    job.getId().toString(),
                    batch,
                    "text-embedding-3-small"
            );

            embeddingRequestEventProducer.sendEmbeddingRequestEvent(batchEvent);
        }

        log.info("EMBEDDING_SERVICE PROCESS END jobId={} projectId={} documentId={} totalChunks={}", job.getId(), event.projectId(), event.documentId(), totalChunks);
    }

    public void processEmbeddingCompletedEvent(EmbeddingCompletedEvent event) {
        log.info("EMBEDDING_SERVICE PROCESS EMBEDDING_COMPLETED_EVENT START jobId={} totalChunks={}", event.ingestionJobId(), event.embeddedChunks().size());
        EmbeddingJob job = jobRepository.findById(UUID.fromString(event.ingestionJobId()))
                .orElseThrow(() -> {
                    log.error("EMBEDDING_SERVICE PROCESS EMBEDDING_COMPLETED_EVENT FAILED reason=job_not_found jobId={} totalChunks={}", event.ingestionJobId(), event.embeddedChunks().size());
                    return new RuntimeException("Job not found");
                });

        ChunkMetadata metadata = new ChunkMetadata(
                job.getWorkspaceId(),
                job.getProjectId()
        );

        vectorStorageService.upsert(event.embeddedChunks(), metadata);

        log.info("EMBEDDING_SERVICE PROCESS EMBEDDING_COMPLETED_EVENT END jobId={} totalChunks={}", event.ingestionJobId(), event.embeddedChunks().size());

        job.setProcessedChunks(job.getProcessedChunks() + event.embeddedChunks().size());
        if (job.getProcessedChunks() == job.getTotalChunks()) {
            job.setStatus(EmbeddingStatus.COMPLETED);
            job.setModelUsed(event.model());
            log.info("EMBEDDING_SERVICE PROCESS EMBEDDING_COMPLETED_EVENT COMPLETED jobId={} totalChunks={}", event.ingestionJobId(), event.embeddedChunks().size());
        }
        jobRepository.save(job);
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
