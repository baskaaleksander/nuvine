package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorProcessingEventProducerTest {

    @Mock
    private KafkaTemplate<String, VectorProcessingRequestEvent> kafkaTemplate;

    @InjectMocks
    private VectorProcessingEventProducer producer;

    @Captor
    private ArgumentCaptor<Message<VectorProcessingRequestEvent>> messageCaptor;

    private VectorProcessingRequestEvent event;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "topic", "vector-processing-request");

        documentId = UUID.randomUUID();
        List<Chunk> chunks = List.of(
                new Chunk(documentId, 1, 0, 100, "chunk 1 content", 0),
                new Chunk(documentId, 1, 100, 200, "chunk 2 content", 1)
        );

        event = new VectorProcessingRequestEvent(
                UUID.randomUUID().toString(),
                documentId.toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                chunks
        );
    }

    @Test
    void sendVectorProcessingRequestEvent_validEvent_sendsToKafka() {
        producer.sendVectorProcessingRequestEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());
        
        Message<VectorProcessingRequestEvent> capturedMessage = messageCaptor.getValue();
        assertEquals(event, capturedMessage.getPayload());
        assertEquals("vector-processing-request", capturedMessage.getHeaders().get("kafka_topic"));
    }

    @Test
    void sendVectorProcessingRequestEvent_multipleEvents_sendsAll() {
        UUID documentId2 = UUID.randomUUID();
        List<Chunk> chunks2 = List.of(
                new Chunk(documentId2, 1, 0, 150, "chunk a content", 0),
                new Chunk(documentId2, 2, 0, 150, "chunk b content", 1)
        );

        VectorProcessingRequestEvent event2 = new VectorProcessingRequestEvent(
                UUID.randomUUID().toString(),
                documentId2.toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                chunks2
        );

        producer.sendVectorProcessingRequestEvent(event);
        producer.sendVectorProcessingRequestEvent(event2);

        verify(kafkaTemplate, times(2)).send(any(Message.class));
    }
}
