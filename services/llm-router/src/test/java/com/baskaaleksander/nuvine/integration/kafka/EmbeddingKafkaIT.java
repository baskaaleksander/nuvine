package com.baskaaleksander.nuvine.integration.kafka;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class EmbeddingKafkaIT extends BaseKafkaIntegrationTest {

    @Value("${topics.embedding-request-topic}")
    private String embeddingRequestTopic;

    @Value("${topics.embedding-completed-topic}")
    private String embeddingCompletedTopic;

    private WireMockStubs wireMockStubs;

    @BeforeEach
    void setup() {
        wireMockStubs = new WireMockStubs(wireMockServer);
    }

    @Test
    @DisplayName("Should consume EmbeddingRequestEvent, call OpenAI, and publish EmbeddingCompletedEvent")
    void shouldProcessEmbeddingRequestEvent() throws InterruptedException {
        // Given
        UUID documentId = UUID.randomUUID();
        String embeddingJobId = UUID.randomUUID().toString();

        List<Chunk> chunks = List.of(
                new Chunk(documentId, 1, 0, 100, "This is the first chunk of text.", 0),
                new Chunk(documentId, 1, 100, 200, "This is the second chunk of text.", 1)
        );

        List<List<Float>> testEmbeddings = List.of(
                WireMockStubs.generateTestEmbedding(),
                WireMockStubs.generateTestEmbedding()
        );
        wireMockStubs.stubOpenAIEmbedding("text-embedding-3-small", testEmbeddings);

        EmbeddingRequestEvent requestEvent = new EmbeddingRequestEvent(
                embeddingJobId,
                chunks,
                "text-embedding-3-small"
        );

        // Set up consumer for completed events
        BlockingQueue<ConsumerRecord<String, EmbeddingCompletedEvent>> completedEvents =
                createConsumer(embeddingCompletedTopic, EmbeddingCompletedEvent.class);

        // When
        kafkaTemplate.send(embeddingRequestTopic, requestEvent);
        kafkaTemplate.flush();

        // Then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            EmbeddingCompletedEvent completedEvent = awaitMessage(completedEvents, 5, TimeUnit.SECONDS);
            assertThat(completedEvent).isNotNull();
            assertThat(completedEvent.ingestionJobId()).isEqualTo(embeddingJobId);
            assertThat(completedEvent.embeddedChunks()).hasSize(2);
            assertThat(completedEvent.model()).isEqualTo("text-embedding-3-small");

            // Verify embedded chunks have embeddings
            assertThat(completedEvent.embeddedChunks().get(0).embedding()).hasSize(1536);
            assertThat(completedEvent.embeddedChunks().get(1).embedding()).hasSize(1536);

            // Verify chunk metadata is preserved
            assertThat(completedEvent.embeddedChunks().get(0).documentId()).isEqualTo(documentId);
            assertThat(completedEvent.embeddedChunks().get(0).content()).isEqualTo("This is the first chunk of text.");
        });
    }

    @Test
    @DisplayName("Should process single chunk embedding request")
    void shouldProcessSingleChunkEmbedding() throws InterruptedException {
        // Given
        UUID documentId = UUID.randomUUID();
        String embeddingJobId = UUID.randomUUID().toString();

        List<Chunk> chunks = List.of(
                new Chunk(documentId, 1, 0, 50, "Single chunk content.", 0)
        );

        List<Float> testEmbedding = WireMockStubs.generateTestEmbedding();
        wireMockStubs.stubOpenAIEmbeddingSingle("text-embedding-3-small", testEmbedding);

        EmbeddingRequestEvent requestEvent = new EmbeddingRequestEvent(
                embeddingJobId,
                chunks,
                "text-embedding-3-small"
        );

        // Set up consumer for completed events
        BlockingQueue<ConsumerRecord<String, EmbeddingCompletedEvent>> completedEvents =
                createConsumer(embeddingCompletedTopic, EmbeddingCompletedEvent.class);

        // When
        kafkaTemplate.send(embeddingRequestTopic, requestEvent);
        kafkaTemplate.flush();

        // Then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            EmbeddingCompletedEvent completedEvent = awaitMessage(completedEvents, 5, TimeUnit.SECONDS);
            assertThat(completedEvent).isNotNull();
            assertThat(completedEvent.ingestionJobId()).isEqualTo(embeddingJobId);
            assertThat(completedEvent.embeddedChunks()).hasSize(1);
            assertThat(completedEvent.embeddedChunks().getFirst().content()).isEqualTo("Single chunk content.");
        });
    }

    @Test
    @DisplayName("Should process large batch of chunks (10 chunks)")
    void shouldProcessLargeBatchOfChunks() throws InterruptedException {
        // Given
        UUID documentId = UUID.randomUUID();
        String embeddingJobId = UUID.randomUUID().toString();

        List<Chunk> chunks = new ArrayList<>();
        List<List<Float>> testEmbeddings = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            chunks.add(new Chunk(documentId, i + 1, i * 100, (i + 1) * 100, "Chunk number " + (i + 1), i));
            testEmbeddings.add(WireMockStubs.generateTestEmbedding());
        }

        wireMockStubs.stubOpenAIEmbedding("text-embedding-3-small", testEmbeddings);

        EmbeddingRequestEvent requestEvent = new EmbeddingRequestEvent(
                embeddingJobId,
                chunks,
                "text-embedding-3-small"
        );

        // Set up consumer for completed events
        BlockingQueue<ConsumerRecord<String, EmbeddingCompletedEvent>> completedEvents =
                createConsumer(embeddingCompletedTopic, EmbeddingCompletedEvent.class);

        // When
        kafkaTemplate.send(embeddingRequestTopic, requestEvent);
        kafkaTemplate.flush();

        // Then
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            EmbeddingCompletedEvent completedEvent = awaitMessage(completedEvents, 5, TimeUnit.SECONDS);
            assertThat(completedEvent).isNotNull();
            assertThat(completedEvent.ingestionJobId()).isEqualTo(embeddingJobId);
            assertThat(completedEvent.embeddedChunks()).hasSize(10);

            // Verify all chunks have embeddings
            for (int i = 0; i < 10; i++) {
                assertThat(completedEvent.embeddedChunks().get(i).embedding()).hasSize(1536);
                assertThat(completedEvent.embeddedChunks().get(i).index()).isEqualTo(i);
            }
        });
    }

    @Test
    @DisplayName("Should not publish completed event when OpenAI API returns error")
    void shouldNotPublishEventOnOpenAIError() throws InterruptedException {
        // Given
        UUID documentId = UUID.randomUUID();
        String embeddingJobId = UUID.randomUUID().toString();

        List<Chunk> chunks = List.of(
                new Chunk(documentId, 1, 0, 50, "Test content.", 0)
        );

        wireMockStubs.stubOpenAIError(500, "Internal server error");

        EmbeddingRequestEvent requestEvent = new EmbeddingRequestEvent(
                embeddingJobId,
                chunks,
                "text-embedding-3-small"
        );

        // Set up consumer for completed events
        BlockingQueue<ConsumerRecord<String, EmbeddingCompletedEvent>> completedEvents =
                createConsumer(embeddingCompletedTopic, EmbeddingCompletedEvent.class);

        // When
        kafkaTemplate.send(embeddingRequestTopic, requestEvent);
        kafkaTemplate.flush();

        // Then - no completed event should be published
        EmbeddingCompletedEvent completedEvent = awaitMessage(completedEvents, 5, TimeUnit.SECONDS);
        assertThat(completedEvent).isNull();
    }
}
