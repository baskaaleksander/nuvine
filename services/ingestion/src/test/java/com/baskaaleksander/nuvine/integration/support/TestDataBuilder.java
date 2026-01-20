package com.baskaaleksander.nuvine.integration.support;

import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestDataBuilder {

    private final IngestionJobRepository ingestionJobRepository;

    public TestDataBuilder(IngestionJobRepository ingestionJobRepository) {
        this.ingestionJobRepository = ingestionJobRepository;
    }

    public IngestionJob createQueuedJob(UUID workspaceId, UUID projectId, UUID documentId,
                                        String storageKey, String mimeType) {
        IngestionJob job = IngestionJob.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .storageKey(storageKey)
                .mimeType(mimeType)
                .status(IngestionStatus.QUEUED)
                .stage(IngestionStage.QUEUED)
                .retryCount(0)
                .build();
        return ingestionJobRepository.save(job);
    }

    public IngestionJob createProcessingJob(UUID workspaceId, UUID projectId, UUID documentId,
                                            String storageKey, String mimeType, IngestionStage stage) {
        IngestionJob job = IngestionJob.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .storageKey(storageKey)
                .mimeType(mimeType)
                .status(IngestionStatus.PROCESSING)
                .stage(stage)
                .retryCount(0)
                .build();
        return ingestionJobRepository.save(job);
    }

    public IngestionJob createCompletedJob(UUID workspaceId, UUID projectId, UUID documentId) {
        IngestionJob job = IngestionJob.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .storageKey("completed/" + documentId + "/file.txt")
                .mimeType("text/plain")
                .status(IngestionStatus.COMPLETED)
                .stage(IngestionStage.EMBED)
                .retryCount(0)
                .build();
        return ingestionJobRepository.save(job);
    }

    public IngestionJob createFailedJob(UUID workspaceId, UUID projectId, UUID documentId,
                                        String lastError, IngestionStage failedStage) {
        IngestionJob job = IngestionJob.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .documentId(documentId)
                .storageKey("failed/" + documentId + "/file.txt")
                .mimeType("text/plain")
                .status(IngestionStatus.FAILED)
                .stage(failedStage)
                .lastError(lastError)
                .retryCount(1)
                .build();
        return ingestionJobRepository.save(job);
    }

    public IngestionJobRepository ingestionJobRepository() {
        return ingestionJobRepository;
    }

    public void cleanUp() {
        ingestionJobRepository.deleteAll();
    }
}
