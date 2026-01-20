package com.baskaaleksander.nuvine.integration.repository;

import com.baskaaleksander.nuvine.domain.model.EmbeddingJob;
import com.baskaaleksander.nuvine.domain.model.EmbeddingStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.EmbeddingJobRepository;
import com.baskaaleksander.nuvine.integration.base.BaseRepositoryIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingJobRepositoryIT extends BaseRepositoryIntegrationTest {

    @Autowired
    private EmbeddingJobRepository embeddingJobRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private UUID workspaceId;
    private UUID projectId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        testDataBuilder.cleanUp();
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
    }

    @Test
    void shouldSaveAndFindEmbeddingJobById() {
        EmbeddingJob job = testDataBuilder.createEmbeddingJob(workspaceId, projectId, documentId, 10);

        Optional<EmbeddingJob> found = embeddingJobRepository.findById(job.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(found.get().getProjectId()).isEqualTo(projectId);
        assertThat(found.get().getDocumentId()).isEqualTo(documentId);
        assertThat(found.get().getTotalChunks()).isEqualTo(10);
        assertThat(found.get().getStatus()).isEqualTo(EmbeddingStatus.IN_PROGRESS);
    }

    @Test
    void shouldUpdateJobStatusAndProcessedChunks() {
        EmbeddingJob job = testDataBuilder.createEmbeddingJob(workspaceId, projectId, documentId, 10);

        job.setProcessedChunks(5);
        embeddingJobRepository.save(job);

        Optional<EmbeddingJob> updated = embeddingJobRepository.findById(job.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getProcessedChunks()).isEqualTo(5);

        job.setProcessedChunks(10);
        job.setStatus(EmbeddingStatus.COMPLETED);
        job.setModelUsed("text-embedding-3-small");
        embeddingJobRepository.save(job);

        updated = embeddingJobRepository.findById(job.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getProcessedChunks()).isEqualTo(10);
        assertThat(updated.get().getStatus()).isEqualTo(EmbeddingStatus.COMPLETED);
        assertThat(updated.get().getModelUsed()).isEqualTo("text-embedding-3-small");
    }

    @Test
    void shouldReturnEmptyWhenJobNotFound() {
        Optional<EmbeddingJob> found = embeddingJobRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void shouldSaveJobWithDifferentStatuses() {
        EmbeddingJob pendingJob = testDataBuilder.createPendingJob(workspaceId, projectId, documentId, 5);
        EmbeddingJob completedJob = testDataBuilder.createCompletedJob(workspaceId, projectId, UUID.randomUUID(), 10);
        EmbeddingJob failedJob = testDataBuilder.createFailedJob(workspaceId, projectId, UUID.randomUUID(), 8);

        assertThat(embeddingJobRepository.findById(pendingJob.getId()).get().getStatus())
                .isEqualTo(EmbeddingStatus.PENDING);
        assertThat(embeddingJobRepository.findById(completedJob.getId()).get().getStatus())
                .isEqualTo(EmbeddingStatus.COMPLETED);
        assertThat(embeddingJobRepository.findById(failedJob.getId()).get().getStatus())
                .isEqualTo(EmbeddingStatus.FAILED);
    }

    @Test
    void shouldPersistIngestionJobId() {
        UUID ingestionJobId = UUID.randomUUID();
        EmbeddingJob job = testDataBuilder.createJobWithIngestionId(
                workspaceId, projectId, documentId, ingestionJobId, 15);

        Optional<EmbeddingJob> found = embeddingJobRepository.findById(job.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getIngestionJobId()).isEqualTo(ingestionJobId);
    }

    @Test
    void shouldGenerateTimestamps() {
        EmbeddingJob job = testDataBuilder.createEmbeddingJob(workspaceId, projectId, documentId, 10);

        Optional<EmbeddingJob> found = embeddingJobRepository.findById(job.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }
}
