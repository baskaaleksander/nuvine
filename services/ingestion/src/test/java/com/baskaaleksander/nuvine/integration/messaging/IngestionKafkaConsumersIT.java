package com.baskaaleksander.nuvine.integration.messaging;

import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStage;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.S3TestUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class IngestionKafkaConsumersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private IngestionJobRepository ingestionJobRepository;

    @Autowired
    private S3Client s3Client;

    @Value("${s3.bucket-name}")
    private String bucketName;

    @Value("${topics.document-uploaded-topic}")
    private String documentUploadedTopic;

    @Value("${topics.vector-processing-request-topic}")
    private String vectorProcessingRequestTopic;

    @Value("${topics.vector-processing-completed-topic}")
    private String vectorProcessingCompletedTopic;

    @Value("${topics.document-ingestion-completed-topic}")
    private String documentIngestionCompletedTopic;

    private S3TestUtils s3TestUtils;

    @BeforeEach
    void setUp() {
        s3TestUtils = new S3TestUtils(s3Client, bucketName);
        s3TestUtils.ensureBucketExists();
        ingestionJobRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        s3TestUtils.cleanBucket();
        ingestionJobRepository.deleteAll();
    }

    @Nested
    @DisplayName("DocumentUploadedEventConsumer")
    class DocumentUploadedEventConsumerTests {

        @Test
        @DisplayName("should process event and produce VectorProcessingRequestEvent for text/plain")
        void shouldProcessEventAndProduceVectorProcessingRequestEventForTextPlain() throws InterruptedException {
            BlockingQueue<ConsumerRecord<String, VectorProcessingRequestEvent>> queue =
                    createConsumer(vectorProcessingRequestTopic, VectorProcessingRequestEvent.class);

            String documentId = UUID.randomUUID().toString();
            String workspaceId = UUID.randomUUID().toString();
            String projectId = UUID.randomUUID().toString();
            String storageKey = "test/" + documentId + "/document.txt";

            s3TestUtils.uploadTestDocument(storageKey,
                    "This is a test document for integration testing. " +
                    "It contains multiple sentences to be chunked. " +
                    "The chunker should process this content correctly.");

            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    documentId, workspaceId, projectId, storageKey, "text/plain", 150L
            );

            kafkaTemplate.send(documentUploadedTopic, event);

            VectorProcessingRequestEvent received = awaitMessage(queue, 30, TimeUnit.SECONDS);

            assertThat(received).isNotNull();
            assertThat(received.documentId()).isEqualTo(documentId);
            assertThat(received.workspaceId()).isEqualTo(workspaceId);
            assertThat(received.projectId()).isEqualTo(projectId);
            assertThat(received.chunks()).isNotEmpty();

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                Optional<IngestionJob> job = ingestionJobRepository.findByDocumentId(UUID.fromString(documentId));
                assertThat(job).isPresent();
                assertThat(job.get().getStatus()).isEqualTo(IngestionStatus.PROCESSING);
                assertThat(job.get().getStage()).isEqualTo(IngestionStage.EMBED);
            });
        }

        @Test
        @DisplayName("should process event and produce VectorProcessingRequestEvent for text/markdown")
        void shouldProcessEventAndProduceVectorProcessingRequestEventForMarkdown() throws InterruptedException {
            BlockingQueue<ConsumerRecord<String, VectorProcessingRequestEvent>> queue =
                    createConsumer(vectorProcessingRequestTopic, VectorProcessingRequestEvent.class);

            String documentId = UUID.randomUUID().toString();
            String workspaceId = UUID.randomUUID().toString();
            String projectId = UUID.randomUUID().toString();
            String storageKey = "test/" + documentId + "/document.md";

            s3TestUtils.uploadTestDocumentFromResource(storageKey, "test-documents/sample.md", "text/markdown");

            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    documentId, workspaceId, projectId, storageKey, "text/markdown", 500L
            );

            kafkaTemplate.send(documentUploadedTopic, event);

            VectorProcessingRequestEvent received = awaitMessage(queue, 30, TimeUnit.SECONDS);

            assertThat(received).isNotNull();
            assertThat(received.documentId()).isEqualTo(documentId);
            assertThat(received.chunks()).isNotEmpty();
        }

        @Test
        @DisplayName("should create IngestionJob on first event")
        void shouldCreateIngestionJobOnFirstEvent() throws InterruptedException {
            BlockingQueue<ConsumerRecord<String, VectorProcessingRequestEvent>> queue =
                    createConsumer(vectorProcessingRequestTopic, VectorProcessingRequestEvent.class);

            String documentId = UUID.randomUUID().toString();
            String workspaceId = UUID.randomUUID().toString();
            String projectId = UUID.randomUUID().toString();
            String storageKey = "test/" + documentId + "/document.txt";

            s3TestUtils.uploadTestDocument(storageKey, "Simple test content for creating a job.");

            assertThat(ingestionJobRepository.findByDocumentId(UUID.fromString(documentId))).isEmpty();

            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    documentId, workspaceId, projectId, storageKey, "text/plain", 50L
            );

            kafkaTemplate.send(documentUploadedTopic, event);

            awaitMessage(queue, 30, TimeUnit.SECONDS);

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                Optional<IngestionJob> job = ingestionJobRepository.findByDocumentId(UUID.fromString(documentId));
                assertThat(job).isPresent();
                assertThat(job.get().getDocumentId()).isEqualTo(UUID.fromString(documentId));
                assertThat(job.get().getWorkspaceId()).isEqualTo(UUID.fromString(workspaceId));
                assertThat(job.get().getProjectId()).isEqualTo(UUID.fromString(projectId));
                assertThat(job.get().getStorageKey()).isEqualTo(storageKey);
                assertThat(job.get().getMimeType()).isEqualTo("text/plain");
            });
        }
    }

    @Nested
    @DisplayName("VectorProcessingCompletedEventConsumer")
    class VectorProcessingCompletedEventConsumerTests {

        @Test
        @DisplayName("should complete job and produce DocumentIngestionCompletedEvent")
        void shouldCompleteJobAndProduceDocumentIngestionCompletedEvent() throws InterruptedException {
            BlockingQueue<ConsumerRecord<String, DocumentIngestionCompletedEvent>> queue =
                    createConsumer(documentIngestionCompletedTopic, DocumentIngestionCompletedEvent.class);

            UUID workspaceId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();
            UUID documentId = UUID.randomUUID();

            IngestionJob job = IngestionJob.builder()
                    .documentId(documentId)
                    .workspaceId(workspaceId)
                    .projectId(projectId)
                    .storageKey("test/" + documentId + "/file.txt")
                    .mimeType("text/plain")
                    .status(IngestionStatus.PROCESSING)
                    .stage(IngestionStage.EMBED)
                    .retryCount(0)
                    .build();
            IngestionJob savedJob = ingestionJobRepository.save(job);

            VectorProcessingCompletedEvent event = new VectorProcessingCompletedEvent(
                    savedJob.getId().toString(),
                    documentId.toString(),
                    projectId.toString(),
                    workspaceId.toString()
            );

            kafkaTemplate.send(vectorProcessingCompletedTopic, event);

            DocumentIngestionCompletedEvent received = awaitMessage(queue, 30, TimeUnit.SECONDS);

            assertThat(received).isNotNull();
            assertThat(received.documentId()).isEqualTo(documentId.toString());
            assertThat(received.workspaceId()).isEqualTo(workspaceId.toString());
            assertThat(received.projectId()).isEqualTo(projectId.toString());

            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                Optional<IngestionJob> completedJob = ingestionJobRepository.findById(savedJob.getId());
                assertThat(completedJob).isPresent();
                assertThat(completedJob.get().getStatus()).isEqualTo(IngestionStatus.COMPLETED);
            });
        }

        @Test
        @DisplayName("should handle non-existent job gracefully")
        void shouldHandleNonExistentJobGracefully() throws InterruptedException {
            BlockingQueue<ConsumerRecord<String, DocumentIngestionCompletedEvent>> queue =
                    createConsumer(documentIngestionCompletedTopic, DocumentIngestionCompletedEvent.class);

            String nonExistentJobId = UUID.randomUUID().toString();
            String documentId = UUID.randomUUID().toString();
            String projectId = UUID.randomUUID().toString();
            String workspaceId = UUID.randomUUID().toString();

            VectorProcessingCompletedEvent event = new VectorProcessingCompletedEvent(
                    nonExistentJobId, documentId, projectId, workspaceId
            );

            kafkaTemplate.send(vectorProcessingCompletedTopic, event);

            DocumentIngestionCompletedEvent received = awaitMessage(queue, 5, TimeUnit.SECONDS);

            assertThat(received).isNull();
        }
    }
}
