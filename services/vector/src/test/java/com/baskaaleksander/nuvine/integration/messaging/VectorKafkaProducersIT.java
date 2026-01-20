package com.baskaaleksander.nuvine.integration.messaging;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmbeddingRequestEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingCompletedEventProducer;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class VectorKafkaProducersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private EmbeddingRequestEventProducer embeddingRequestEventProducer;

    @Autowired
    private VectorProcessingCompletedEventProducer vectorProcessingCompletedEventProducer;

    @Value("${topics.embedding-request-topic}")
    private String embeddingRequestTopic;

    @Value("${topics.vector-processing-completed-topic}")
    private String vectorProcessingCompletedTopic;

    private UUID workspaceId;
    private UUID projectId;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
    }

    @Test
    void shouldPublishEmbeddingRequestEvent() throws InterruptedException {
        BlockingQueue<ConsumerRecord<String, EmbeddingRequestEvent>> records =
                createConsumer(embeddingRequestTopic, EmbeddingRequestEvent.class);

        UUID embeddingJobId = UUID.randomUUID();
        List<Chunk> chunks = createTestChunks(3);

        EmbeddingRequestEvent event = new EmbeddingRequestEvent(
                embeddingJobId.toString(),
                chunks,
                "text-embedding-3-small"
        );

        embeddingRequestEventProducer.sendEmbeddingRequestEvent(event);

        EmbeddingRequestEvent receivedEvent = awaitMessage(records, 30, TimeUnit.SECONDS);

        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.embeddingJobId()).isEqualTo(embeddingJobId.toString());
        assertThat(receivedEvent.chunks()).hasSize(3);
        assertThat(receivedEvent.model()).isEqualTo("text-embedding-3-small");
    }

    @Test
    void shouldPublishVectorProcessingCompletedEvent() throws InterruptedException {
        BlockingQueue<ConsumerRecord<String, VectorProcessingCompletedEvent>> records =
                createConsumer(vectorProcessingCompletedTopic, VectorProcessingCompletedEvent.class);

        UUID ingestionJobId = UUID.randomUUID();

        VectorProcessingCompletedEvent event = new VectorProcessingCompletedEvent(
                ingestionJobId.toString(),
                documentId.toString(),
                projectId.toString(),
                workspaceId.toString()
        );

        vectorProcessingCompletedEventProducer.sendVectorProcessingCompletedEvent(event);

        VectorProcessingCompletedEvent receivedEvent = awaitMessage(records, 30, TimeUnit.SECONDS);

        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.ingestionJobId()).isEqualTo(ingestionJobId.toString());
        assertThat(receivedEvent.documentId()).isEqualTo(documentId.toString());
        assertThat(receivedEvent.projectId()).isEqualTo(projectId.toString());
        assertThat(receivedEvent.workspaceId()).isEqualTo(workspaceId.toString());
    }

    @Test
    void shouldPublishMultipleEmbeddingRequestEvents() throws InterruptedException {
        BlockingQueue<ConsumerRecord<String, EmbeddingRequestEvent>> records =
                createConsumer(embeddingRequestTopic, EmbeddingRequestEvent.class);

        for (int i = 0; i < 3; i++) {
            UUID embeddingJobId = UUID.randomUUID();
            List<Chunk> chunks = createTestChunks(2);

            EmbeddingRequestEvent event = new EmbeddingRequestEvent(
                    embeddingJobId.toString(),
                    chunks,
                    "text-embedding-3-small"
            );

            embeddingRequestEventProducer.sendEmbeddingRequestEvent(event);
        }

        int receivedCount = 0;
        for (int i = 0; i < 3; i++) {
            EmbeddingRequestEvent receivedEvent = awaitMessage(records, 30, TimeUnit.SECONDS);
            if (receivedEvent != null) {
                receivedCount++;
                assertThat(receivedEvent.chunks()).hasSize(2);
                assertThat(receivedEvent.model()).isEqualTo("text-embedding-3-small");
            }
        }

        assertThat(receivedCount).isEqualTo(3);
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
}
