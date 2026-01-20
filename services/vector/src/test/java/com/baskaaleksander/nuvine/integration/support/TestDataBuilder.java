package com.baskaaleksander.nuvine.integration.support;

import com.baskaaleksander.nuvine.domain.model.EmbeddingJob;
import com.baskaaleksander.nuvine.domain.model.EmbeddingStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.EmbeddingJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestDataBuilder {

    @Autowired
    private EmbeddingJobRepository embeddingJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public EmbeddingJob createEmbeddingJob(UUID workspaceId, UUID projectId, UUID documentId, int totalChunks) {
        EmbeddingJob job = EmbeddingJob.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .ingestionJobId(UUID.randomUUID())
                .status(EmbeddingStatus.IN_PROGRESS)
                .totalChunks(totalChunks)
                .processedChunks(0)
                .build();
        return embeddingJobRepository.save(job);
    }

    public EmbeddingJob createPendingJob(UUID workspaceId, UUID projectId, UUID documentId, int totalChunks) {
        EmbeddingJob job = EmbeddingJob.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .ingestionJobId(UUID.randomUUID())
                .status(EmbeddingStatus.PENDING)
                .totalChunks(totalChunks)
                .processedChunks(0)
                .build();
        return embeddingJobRepository.save(job);
    }

    public EmbeddingJob createCompletedJob(UUID workspaceId, UUID projectId, UUID documentId, int totalChunks) {
        EmbeddingJob job = EmbeddingJob.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .ingestionJobId(UUID.randomUUID())
                .status(EmbeddingStatus.COMPLETED)
                .totalChunks(totalChunks)
                .processedChunks(totalChunks)
                .modelUsed("text-embedding-3-small")
                .build();
        return embeddingJobRepository.save(job);
    }

    public EmbeddingJob createFailedJob(UUID workspaceId, UUID projectId, UUID documentId, int totalChunks) {
        EmbeddingJob job = EmbeddingJob.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .ingestionJobId(UUID.randomUUID())
                .status(EmbeddingStatus.FAILED)
                .totalChunks(totalChunks)
                .processedChunks(0)
                .build();
        return embeddingJobRepository.save(job);
    }

    public EmbeddingJob createJobWithIngestionId(UUID workspaceId, UUID projectId, UUID documentId,
                                                  UUID ingestionJobId, int totalChunks) {
        EmbeddingJob job = EmbeddingJob.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .ingestionJobId(ingestionJobId)
                .status(EmbeddingStatus.IN_PROGRESS)
                .totalChunks(totalChunks)
                .processedChunks(0)
                .build();
        return embeddingJobRepository.save(job);
    }

    public void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE embedding_jobs CASCADE");
    }
}
