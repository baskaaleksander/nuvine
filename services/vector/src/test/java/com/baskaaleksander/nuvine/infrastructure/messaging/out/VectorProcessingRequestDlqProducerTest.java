package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestDlqMessage;
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
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VectorProcessingRequestDlqProducerTest {

    private static final String DLQ_TOPIC = "vector-processing-request-dlq";
    private static final String DEAD_LETTER_TOPIC = "vector-processing-request-dead-letter";
    private static final String INGESTION_JOB_ID = "job-123";

    @Mock
    private KafkaTemplate<String, VectorProcessingRequestDlqMessage> kafkaTemplate;

    @InjectMocks
    private VectorProcessingRequestDlqProducer producer;

    @Captor
    private ArgumentCaptor<Message<VectorProcessingRequestDlqMessage>> messageCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "dlqTopic", DLQ_TOPIC);
        ReflectionTestUtils.setField(producer, "deadLetterTopic", DEAD_LETTER_TOPIC);
    }

    @Test
    void sendToDlq_sendsMessageToDlqTopic() {
        VectorProcessingRequestDlqMessage dlqMessage = createDlqMessage();

        producer.sendToDlq(dlqMessage);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<VectorProcessingRequestDlqMessage> capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getPayload()).isEqualTo(dlqMessage);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(DLQ_TOPIC);
    }

    @Test
    void sendToDlq_setsMessageKey() {
        VectorProcessingRequestDlqMessage dlqMessage = createDlqMessage();

        producer.sendToDlq(dlqMessage);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<VectorProcessingRequestDlqMessage> capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.KEY)).isEqualTo(INGESTION_JOB_ID);
    }

    @Test
    void sendToDeadLetter_sendsMessageToDeadLetterTopic() {
        VectorProcessingRequestDlqMessage dlqMessage = createDlqMessage();

        producer.sendToDeadLetter(dlqMessage);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<VectorProcessingRequestDlqMessage> capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getPayload()).isEqualTo(dlqMessage);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(DEAD_LETTER_TOPIC);
    }

    @Test
    void sendToDeadLetter_setsMessageKey() {
        VectorProcessingRequestDlqMessage dlqMessage = createDlqMessage();

        producer.sendToDeadLetter(dlqMessage);

        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<VectorProcessingRequestDlqMessage> capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.KEY)).isEqualTo(INGESTION_JOB_ID);
    }

    private VectorProcessingRequestDlqMessage createDlqMessage() {
        VectorProcessingRequestEvent originalEvent = new VectorProcessingRequestEvent(
                INGESTION_JOB_ID,
                "doc-456",
                "project-789",
                "workspace-abc",
                List.of()
        );

        return new VectorProcessingRequestDlqMessage(
                originalEvent,
                1,
                "Test error message",
                "java.lang.RuntimeException",
                Instant.now(),
                Instant.now(),
                "original-topic"
        );
    }
}
