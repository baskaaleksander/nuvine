package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedEvent;
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

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorProcessingCompletedDlqProducerTest {

    @Mock
    private KafkaTemplate<String, VectorProcessingCompletedDlqMessage> kafkaTemplate;

    @InjectMocks
    private VectorProcessingCompletedDlqProducer producer;

    @Captor
    private ArgumentCaptor<Message<VectorProcessingCompletedDlqMessage>> messageCaptor;

    private VectorProcessingCompletedDlqMessage dlqMessage;
    private String ingestionJobId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "dlqTopic", "vector-processing-completed-dlq");
        ReflectionTestUtils.setField(producer, "deadLetterTopic", "vector-processing-completed-dead-letter");

        ingestionJobId = UUID.randomUUID().toString();
        VectorProcessingCompletedEvent event = new VectorProcessingCompletedEvent(
                ingestionJobId,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );

        dlqMessage = new VectorProcessingCompletedDlqMessage(
                event,
                1,
                "Test error",
                "java.lang.RuntimeException",
                Instant.now().minusSeconds(60),
                Instant.now(),
                "vector-processing-completed"
        );
    }

    @Test
    void sendToDlq_validMessage_sendsToCorrectTopic() {
        producer.sendToDlq(dlqMessage);

        verify(kafkaTemplate).send(messageCaptor.capture());
        
        Message<VectorProcessingCompletedDlqMessage> capturedMessage = messageCaptor.getValue();
        assertEquals(dlqMessage, capturedMessage.getPayload());
        assertEquals("vector-processing-completed-dlq", capturedMessage.getHeaders().get("kafka_topic"));
        assertEquals(ingestionJobId, capturedMessage.getHeaders().get("kafka_messageKey"));
    }

    @Test
    void sendToDeadLetter_validMessage_sendsToDeadLetterTopic() {
        producer.sendToDeadLetter(dlqMessage);

        verify(kafkaTemplate).send(messageCaptor.capture());
        
        Message<VectorProcessingCompletedDlqMessage> capturedMessage = messageCaptor.getValue();
        assertEquals(dlqMessage, capturedMessage.getPayload());
        assertEquals("vector-processing-completed-dead-letter", capturedMessage.getHeaders().get("kafka_topic"));
        assertEquals(ingestionJobId, capturedMessage.getHeaders().get("kafka_messageKey"));
    }
}
