package com.baskaaleksander.nuvine.integration.messaging;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentIngestionCompletedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.DocumentUploadedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingEventProducer;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionKafkaProducersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private DocumentUploadedEventProducer documentUploadedEventProducer;

    @Autowired
    private VectorProcessingEventProducer vectorProcessingEventProducer;

    @Autowired
    private DocumentIngestionCompletedEventProducer documentIngestionCompletedEventProducer;

    @Value("${topics.document-uploaded-topic}")
    private String documentUploadedTopic;

    @Value("${topics.vector-processing-request-topic}")
    private String vectorProcessingRequestTopic;

    @Value("${topics.document-ingestion-completed-topic}")
    private String documentIngestionCompletedTopic;

    @Nested
    @DisplayName("DocumentUploadedEventProducer")
    class DocumentUploadedEventProducerTests {

        @Test
        @DisplayName("should produce DocumentUploadedEvent correctly")
        void shouldProduceDocumentUploadedEventCorrectly() throws InterruptedException {
            BlockingQueue<ConsumerRecord<String, DocumentUploadedEvent>> queue =
                    createConsumer(documentUploadedTopic, DocumentUploadedEvent.class);

            String documentId = UUID.randomUUID().toString();
            String workspaceId = UUID.randomUUID().toString();
            String projectId = UUID.randomUUID().toString();
            String storageKey = "test/storage/key.txt";
            String mimeType = "text/plain";
            long sizeBytes = 1024L;

            DocumentUploadedEvent event = new DocumentUploadedEvent(
                    documentId, workspaceId, projectId, storageKey, mimeType, sizeBytes
            );

            documentUploadedEventProducer.sendDocumentUploadedEvent(event);

            DocumentUploadedEvent received = awaitMessage(queue, 10, TimeUnit.SECONDS);

            assertThat(received).isNotNull();
            assertThat(received.documentId()).isEqualTo(documentId);
            assertThat(received.workspaceId()).isEqualTo(workspaceId);
            assertThat(received.projectId()).isEqualTo(projectId);
            assertThat(received.storageKey()).isEqualTo(storageKey);
            assertThat(received.mimeType()).isEqualTo(mimeType);
            assertThat(received.sizeBytes()).isEqualTo(sizeBytes);
        }
    }

    @Nested
    @DisplayName("VectorProcessingEventProducer")
    class VectorProcessingEventProducerTests {

        @Test
        @DisplayName("should produce VectorProcessingRequestEvent with chunks")
        void shouldProduceVectorProcessingRequestEventWithChunks() throws InterruptedException {
            BlockingQueue<ConsumerRecord<String, VectorProcessingRequestEvent>> queue =
                    createConsumer(vectorProcessingRequestTopic, VectorProcessingRequestEvent.class);

            String ingestionJobId = UUID.randomUUID().toString();
            String documentId = UUID.randomUUID().toString();
            String projectId = UUID.randomUUID().toString();
            String workspaceId = UUID.randomUUID().toString();

            List<Chunk> chunks = List.of(
                    new Chunk(UUID.fromString(documentId), 1, 0, 100, "First chunk content", 0),
                    new Chunk(UUID.fromString(documentId), 1, 100, 200, "Second chunk content", 1),
                    new Chunk(UUID.fromString(documentId), 2, 0, 150, "Third chunk content", 2)
            );

            VectorProcessingRequestEvent event = new VectorProcessingRequestEvent(
                    ingestionJobId, documentId, projectId, workspaceId, chunks
            );

            vectorProcessingEventProducer.sendVectorProcessingRequestEvent(event);

            VectorProcessingRequestEvent received = awaitMessage(queue, 10, TimeUnit.SECONDS);

            assertThat(received).isNotNull();
            assertThat(received.ingestionJobId()).isEqualTo(ingestionJobId);
            assertThat(received.documentId()).isEqualTo(documentId);
            assertThat(received.projectId()).isEqualTo(projectId);
            assertThat(received.workspaceId()).isEqualTo(workspaceId);
            assertThat(received.chunks()).hasSize(3);

            Chunk firstChunk = received.chunks().get(0);
            assertThat(firstChunk.documentId()).isEqualTo(UUID.fromString(documentId));
            assertThat(firstChunk.page()).isEqualTo(1);
            assertThat(firstChunk.startOffset()).isEqualTo(0);
            assertThat(firstChunk.endOffset()).isEqualTo(100);
            assertThat(firstChunk.content()).isEqualTo("First chunk content");
            assertThat(firstChunk.index()).isEqualTo(0);
        }

        @Test
        @DisplayName("should produce VectorProcessingRequestEvent with empty chunks")
        void shouldProduceVectorProcessingRequestEventWithEmptyChunks() throws InterruptedException {
            BlockingQueue<ConsumerRecord<String, VectorProcessingRequestEvent>> queue =
                    createConsumer(vectorProcessingRequestTopic, VectorProcessingRequestEvent.class);

            String ingestionJobId = UUID.randomUUID().toString();
            String documentId = UUID.randomUUID().toString();
            String projectId = UUID.randomUUID().toString();
            String workspaceId = UUID.randomUUID().toString();

            VectorProcessingRequestEvent event = new VectorProcessingRequestEvent(
                    ingestionJobId, documentId, projectId, workspaceId, List.of()
            );

            vectorProcessingEventProducer.sendVectorProcessingRequestEvent(event);

            VectorProcessingRequestEvent received = awaitMessage(queue, 10, TimeUnit.SECONDS);

            assertThat(received).isNotNull();
            assertThat(received.chunks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DocumentIngestionCompletedEventProducer")
    class DocumentIngestionCompletedEventProducerTests {

        @Test
        @DisplayName("should produce DocumentIngestionCompletedEvent correctly")
        void shouldProduceDocumentIngestionCompletedEventCorrectly() throws InterruptedException {
            BlockingQueue<ConsumerRecord<String, DocumentIngestionCompletedEvent>> queue =
                    createConsumer(documentIngestionCompletedTopic, DocumentIngestionCompletedEvent.class);

            String documentId = UUID.randomUUID().toString();
            String workspaceId = UUID.randomUUID().toString();
            String projectId = UUID.randomUUID().toString();

            DocumentIngestionCompletedEvent event = new DocumentIngestionCompletedEvent(
                    documentId, workspaceId, projectId
            );

            documentIngestionCompletedEventProducer.sendDocumentIngestionCompletedEvent(event);

            DocumentIngestionCompletedEvent received = awaitMessage(queue, 10, TimeUnit.SECONDS);

            assertThat(received).isNotNull();
            assertThat(received.documentId()).isEqualTo(documentId);
            assertThat(received.workspaceId()).isEqualTo(workspaceId);
            assertThat(received.projectId()).isEqualTo(projectId);
        }
    }
}
