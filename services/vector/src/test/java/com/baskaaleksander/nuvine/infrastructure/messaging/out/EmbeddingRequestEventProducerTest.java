package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmbeddingRequestEventProducerTest {

    @Mock
    private KafkaTemplate<String, EmbeddingRequestEvent> kafkaTemplate;

    @InjectMocks
    private EmbeddingRequestEventProducer producer;

    private EmbeddingRequestEvent event;
    private String topicName;

    @BeforeEach
    void setUp() {
        topicName = "embedding-request-topic";
        ReflectionTestUtils.setField(producer, "topic", topicName);

        UUID documentId = UUID.randomUUID();
        UUID embeddingJobId = UUID.randomUUID();

        List<Chunk> chunks = List.of(
                new Chunk(documentId, 0, 0, 100, "Test content", 0)
        );

        event = new EmbeddingRequestEvent(
                embeddingJobId.toString(),
                chunks,
                "text-embedding-3-small"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendEmbeddingRequestEvent_sendsMessageWithCorrectTopic() {
        producer.sendEmbeddingRequestEvent(event);

        ArgumentCaptor<Message<EmbeddingRequestEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<EmbeddingRequestEvent> message = messageCaptor.getValue();
        assertEquals(topicName, message.getHeaders().get(KafkaHeaders.TOPIC));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendEmbeddingRequestEvent_sendsCorrectPayload() {
        producer.sendEmbeddingRequestEvent(event);

        ArgumentCaptor<Message<EmbeddingRequestEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<EmbeddingRequestEvent> message = messageCaptor.getValue();
        assertEquals(event, message.getPayload());
        assertEquals(event.embeddingJobId(), message.getPayload().embeddingJobId());
        assertEquals(event.model(), message.getPayload().model());
        assertEquals(1, message.getPayload().chunks().size());
    }
}
