package com.baskaaleksander.nuvine.integration.repository;

import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobSpec;
import com.baskaaleksander.nuvine.integration.base.BaseRepositoryIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(com.baskaaleksander.nuvine.infrastructure.config.PersistenceConfig.class)
class IngestionJobRepositoryIT extends BaseRepositoryIntegrationTest {

    @Autowired
    private IngestionJobRepository repository;

    private UUID workspaceId;
    private UUID projectId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("findByDocumentId")
    class FindByDocumentId {

        @Test
        @DisplayName("should find job by document ID")
        void shouldFindJobByDocumentId() {
            IngestionJob job = createJob(workspaceId, projectId, documentId, IngestionStatus.QUEUED, IngestionStage.QUEUED);
            repository.save(job);

            Optional<IngestionJob> found = repository.findByDocumentId(documentId);

            assertThat(found).isPresent();
            assertThat(found.get().getDocumentId()).isEqualTo(documentId);
            assertThat(found.get().getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(found.get().getProjectId()).isEqualTo(projectId);
        }

        @Test
        @DisplayName("should return empty when document not found")
        void shouldReturnEmptyWhenDocumentNotFound() {
            Optional<IngestionJob> found = repository.findByDocumentId(UUID.randomUUID());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll with Specification")
    class FindAllWithSpecification {

        @Test
        @DisplayName("should filter by workspaceId")
        void shouldFilterByWorkspaceId() {
            UUID otherWorkspaceId = UUID.randomUUID();
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.QUEUED, IngestionStage.QUEUED));
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.PROCESSING, IngestionStage.FETCH));
            repository.save(createJob(otherWorkspaceId, projectId, UUID.randomUUID(), IngestionStatus.QUEUED, IngestionStage.QUEUED));

            Specification<IngestionJob> spec = IngestionJobSpec.hasWorkspaceId(workspaceId);
            Page<IngestionJob> result = repository.findAll(spec, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(job -> job.getWorkspaceId().equals(workspaceId));
        }

        @Test
        @DisplayName("should filter by projectId")
        void shouldFilterByProjectId() {
            UUID otherProjectId = UUID.randomUUID();
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.QUEUED, IngestionStage.QUEUED));
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.PROCESSING, IngestionStage.FETCH));
            repository.save(createJob(workspaceId, otherProjectId, UUID.randomUUID(), IngestionStatus.QUEUED, IngestionStage.QUEUED));

            Specification<IngestionJob> spec = IngestionJobSpec.hasProjectId(projectId);
            Page<IngestionJob> result = repository.findAll(spec, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(job -> job.getProjectId().equals(projectId));
        }

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() {
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.QUEUED, IngestionStage.QUEUED));
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.PROCESSING, IngestionStage.FETCH));
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.COMPLETED, IngestionStage.EMBED));
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.FAILED, IngestionStage.PARSE));

            Specification<IngestionJob> spec = IngestionJobSpec.hasStatus(IngestionStatus.QUEUED);
            Page<IngestionJob> result = repository.findAll(spec, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(IngestionStatus.QUEUED);
        }

        @Test
        @DisplayName("should filter with combined specifications")
        void shouldFilterWithCombinedSpecifications() {
            UUID otherWorkspaceId = UUID.randomUUID();
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.QUEUED, IngestionStage.QUEUED));
            repository.save(createJob(workspaceId, projectId, UUID.randomUUID(), IngestionStatus.PROCESSING, IngestionStage.FETCH));
            repository.save(createJob(otherWorkspaceId, projectId, UUID.randomUUID(), IngestionStatus.QUEUED, IngestionStage.QUEUED));

            Specification<IngestionJob> spec = Specification
                    .where(IngestionJobSpec.hasWorkspaceId(workspaceId))
                    .and(IngestionJobSpec.hasStatus(IngestionStatus.QUEUED));

            Page<IngestionJob> result = repository.findAll(spec, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(IngestionStatus.QUEUED);
        }
    }

    @Nested
    @DisplayName("Persist and retrieve all fields")
    class PersistAndRetrieveAllFields {

        @Test
        @DisplayName("should persist and retrieve all fields correctly")
        void shouldPersistAndRetrieveAllFieldsCorrectly() {
            IngestionJob job = IngestionJob.builder()
                    .documentId(documentId)
                    .workspaceId(workspaceId)
                    .projectId(projectId)
                    .storageKey("test/storage/key.txt")
                    .mimeType("text/plain")
                    .status(IngestionStatus.FAILED)
                    .stage(IngestionStage.PARSE)
                    .retryCount(3)
                    .lastError("Test error message")
                    .createdBy(UUID.randomUUID())
                    .build();

            IngestionJob saved = repository.save(job);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getVersion()).isNotNull();

            Optional<IngestionJob> found = repository.findById(saved.getId());

            assertThat(found).isPresent();
            IngestionJob retrieved = found.get();
            assertThat(retrieved.getDocumentId()).isEqualTo(documentId);
            assertThat(retrieved.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(retrieved.getProjectId()).isEqualTo(projectId);
            assertThat(retrieved.getStorageKey()).isEqualTo("test/storage/key.txt");
            assertThat(retrieved.getMimeType()).isEqualTo("text/plain");
            assertThat(retrieved.getStatus()).isEqualTo(IngestionStatus.FAILED);
            assertThat(retrieved.getStage()).isEqualTo(IngestionStage.PARSE);
            assertThat(retrieved.getRetryCount()).isEqualTo(3);
            assertThat(retrieved.getLastError()).isEqualTo("Test error message");
        }
    }

    private IngestionJob createJob(UUID workspaceId, UUID projectId, UUID documentId,
                                   IngestionStatus status, IngestionStage stage) {
        return IngestionJob.builder()
                .documentId(documentId)
                .workspaceId(workspaceId)
                .projectId(projectId)
                .storageKey("test/" + documentId + "/file.txt")
                .mimeType("text/plain")
                .status(status)
                .stage(stage)
                .retryCount(0)
                .build();
    }
}
