package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedEvent;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VectorProcessingCompletedEventProducerTest {

    @Mock
    private KafkaTemplate<String, VectorProcessingCompletedEvent> kafkaTemplate;

    @InjectMocks
    private VectorProcessingCompletedEventProducer producer;

    private VectorProcessingCompletedEvent event;
    private String topicName;

    @BeforeEach
    void setUp() {
        topicName = "vector-processing-completed-topic";
        ReflectionTestUtils.setField(producer, "topic", topicName);

        event = new VectorProcessingCompletedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendVectorProcessingCompletedEvent_sendsMessageWithCorrectTopic() {
        producer.sendVectorProcessingCompletedEvent(event);

        ArgumentCaptor<Message<VectorProcessingCompletedEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<VectorProcessingCompletedEvent> message = messageCaptor.getValue();
        assertEquals(topicName, message.getHeaders().get(KafkaHeaders.TOPIC));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendVectorProcessingCompletedEvent_sendsCorrectPayload() {
        producer.sendVectorProcessingCompletedEvent(event);

        ArgumentCaptor<Message<VectorProcessingCompletedEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<VectorProcessingCompletedEvent> message = messageCaptor.getValue();
        assertEquals(event, message.getPayload());
        assertEquals(event.ingestionJobId(), message.getPayload().ingestionJobId());
        assertEquals(event.documentId(), message.getPayload().documentId());
        assertEquals(event.projectId(), message.getPayload().projectId());
        assertEquals(event.workspaceId(), message.getPayload().workspaceId());
    }
}
