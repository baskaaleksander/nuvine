package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingCompletedEventProducerTest {

    @Mock
    private KafkaTemplate<String, EmbeddingCompletedEvent> kafkaTemplate;

    @InjectMocks
    private EmbeddingCompletedEventProducer producer;

    private String topic;
    private EmbeddingCompletedEvent event;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        topic = "embedding-completed-topic";
        ReflectionTestUtils.setField(producer, "topic", topic);

        documentId = UUID.randomUUID();
        EmbeddedChunk chunk1 = new EmbeddedChunk(documentId, 1, 0, 100, List.of(0.1f, 0.2f), "content1", 0);
        EmbeddedChunk chunk2 = new EmbeddedChunk(documentId, 1, 100, 200, List.of(0.3f, 0.4f), "content2", 1);
        
        event = new EmbeddingCompletedEvent(
                "job-123",
                List.of(chunk1, chunk2),
                "text-embedding-3-small"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendEmbeddingCompletedEvent_sendsToCorrectTopic() {
        producer.sendEmbeddingCompletedEvent(event);

        ArgumentCaptor<Message<EmbeddingCompletedEvent>> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());

        Message<EmbeddingCompletedEvent> capturedMessage = captor.getValue();
        assertEquals(topic, capturedMessage.getHeaders().get(KafkaHeaders.TOPIC));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendEmbeddingCompletedEvent_includesAllChunkData() {
        producer.sendEmbeddingCompletedEvent(event);

        ArgumentCaptor<Message<EmbeddingCompletedEvent>> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());

        Message<EmbeddingCompletedEvent> capturedMessage = captor.getValue();
        EmbeddingCompletedEvent payload = capturedMessage.getPayload();
        
        assertEquals("job-123", payload.ingestionJobId());
        assertEquals("text-embedding-3-small", payload.model());
        assertEquals(2, payload.embeddedChunks().size());
        
        EmbeddedChunk firstChunk = payload.embeddedChunks().get(0);
        assertEquals(documentId, firstChunk.documentId());
        assertEquals(1, firstChunk.page());
        assertEquals(0, firstChunk.startOffset());
        assertEquals(100, firstChunk.endOffset());
        assertEquals(List.of(0.1f, 0.2f), firstChunk.embedding());
        assertEquals("content1", firstChunk.content());
        assertEquals(0, firstChunk.index());
    }
}
