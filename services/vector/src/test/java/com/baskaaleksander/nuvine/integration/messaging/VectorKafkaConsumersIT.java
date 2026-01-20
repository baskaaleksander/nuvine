package com.baskaaleksander.nuvine.integration.messaging;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.domain.model.EmbeddingJob;
import com.baskaaleksander.nuvine.domain.model.EmbeddingStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.EmbeddingJobRepository;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class VectorKafkaConsumersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private EmbeddingJobRepository embeddingJobRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    @Value("${topics.vector-processing-request-topic}")
    private String vectorProcessingRequestTopic;

    @Value("${topics.embedding-completed-topic}")
    private String embeddingCompletedTopic;

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
    void shouldConsumeVectorProcessingRequestEventAndCreateEmbeddingJob() {
        UUID ingestionJobId = UUID.randomUUID();
        List<Chunk> chunks = createTestChunks(5);

        VectorProcessingRequestEvent event = new VectorProcessingRequestEvent(
                ingestionJobId.toString(),
                documentId.toString(),
                projectId.toString(),
                workspaceId.toString(),
                chunks
        );

        ensureTopicExists(vectorProcessingRequestTopic);
        kafkaTemplate.send(vectorProcessingRequestTopic, event);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EmbeddingJob> jobs = embeddingJobRepository.findAll();
            assertThat(jobs).isNotEmpty();

            EmbeddingJob createdJob = jobs.stream()
                    .filter(j -> j.getIngestionJobId().equals(ingestionJobId))
                    .findFirst()
                    .orElse(null);

            assertThat(createdJob).isNotNull();
            assertThat(createdJob.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(createdJob.getProjectId()).isEqualTo(projectId);
            assertThat(createdJob.getDocumentId()).isEqualTo(documentId);
            assertThat(createdJob.getTotalChunks()).isEqualTo(5);
            assertThat(createdJob.getStatus()).isEqualTo(EmbeddingStatus.IN_PROGRESS);
        });
    }

    @Test
    void shouldConsumeEmbeddingCompletedEventAndUpdateJob() {
        EmbeddingJob existingJob = testDataBuilder.createEmbeddingJob(workspaceId, projectId, documentId, 5);

        List<EmbeddedChunk> embeddedChunks = createEmbeddedChunks(5);

        EmbeddingCompletedEvent event = new EmbeddingCompletedEvent(
                existingJob.getId().toString(),
                embeddedChunks,
                "text-embedding-3-small"
        );

        ensureTopicExists(embeddingCompletedTopic);
        kafkaTemplate.send(embeddingCompletedTopic, event);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            EmbeddingJob updatedJob = embeddingJobRepository.findById(existingJob.getId()).orElseThrow();
            assertThat(updatedJob.getProcessedChunks()).isEqualTo(5);
            assertThat(updatedJob.getStatus()).isEqualTo(EmbeddingStatus.COMPLETED);
            assertThat(updatedJob.getModelUsed()).isEqualTo("text-embedding-3-small");
        });
    }

    @Test
    void shouldHandlePartialChunkProcessing() {
        EmbeddingJob existingJob = testDataBuilder.createEmbeddingJob(workspaceId, projectId, documentId, 10);

        List<EmbeddedChunk> firstBatch = createEmbeddedChunks(5);
        EmbeddingCompletedEvent firstEvent = new EmbeddingCompletedEvent(
                existingJob.getId().toString(),
                firstBatch,
                "text-embedding-3-small"
        );

        ensureTopicExists(embeddingCompletedTopic);
        kafkaTemplate.send(embeddingCompletedTopic, firstEvent);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            EmbeddingJob partialJob = embeddingJobRepository.findById(existingJob.getId()).orElseThrow();
            assertThat(partialJob.getProcessedChunks()).isEqualTo(5);
            assertThat(partialJob.getStatus()).isEqualTo(EmbeddingStatus.IN_PROGRESS);
        });

        List<EmbeddedChunk> secondBatch = createEmbeddedChunksWithOffset(5, 5);
        EmbeddingCompletedEvent secondEvent = new EmbeddingCompletedEvent(
                existingJob.getId().toString(),
                secondBatch,
                "text-embedding-3-small"
        );

        kafkaTemplate.send(embeddingCompletedTopic, secondEvent);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            EmbeddingJob completedJob = embeddingJobRepository.findById(existingJob.getId()).orElseThrow();
            assertThat(completedJob.getProcessedChunks()).isEqualTo(10);
            assertThat(completedJob.getStatus()).isEqualTo(EmbeddingStatus.COMPLETED);
        });
    }

    private List<Chunk> createTestChunks(int count) {
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(new Chunk(
                    documentId,
                    i / 2,
                    i * 100,
                    (i + 1) * 100,
                    "Test content for chunk " + i,
                    i
            ));
        }
        return chunks;
    }

    private List<EmbeddedChunk> createEmbeddedChunks(int count) {
        return createEmbeddedChunksWithOffset(count, 0);
    }

    private List<EmbeddedChunk> createEmbeddedChunksWithOffset(int count, int offset) {
        List<EmbeddedChunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = offset + i;
            chunks.add(new EmbeddedChunk(
                    documentId,
                    index / 2,
                    index * 100,
                    (index + 1) * 100,
                    generateMockEmbedding(1536),
                    "Test content for chunk " + index,
                    index
            ));
        }
        return chunks;
    }

    private List<Float> generateMockEmbedding(int dimensions) {
        List<Float> embedding = new ArrayList<>();
        for (int i = 0; i < dimensions; i++) {
            embedding.add((float) Math.random());
        }
        return embedding;
    }
}
