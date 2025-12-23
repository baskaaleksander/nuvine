package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
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
class EmbeddingCompletedDlqProducerTest {

    private static final String DLQ_TOPIC = "embedding-completed-dlq";
    private static final String DEAD_LETTER_TOPIC = "embedding-completed-dead-letter";
    private static final String INGESTION_JOB_ID = "job-123";

    @Mock
    private KafkaTemplate<String, EmbeddingCompletedDlqMessage> kafkaTemplate;

    @InjectMocks
    private EmbeddingCompletedDlqProducer producer;

    @Captor
    private ArgumentCaptor<Message<EmbeddingCompletedDlqMessage>> messageCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "dlqTopic", DLQ_TOPIC);
        ReflectionTestUtils.setField(producer, "deadLetterTopic", DEAD_LETTER_TOPIC);
    }

    @Test
    void sendToDlq_sendsMessageToDlqTopic() {
        // Arrange
        EmbeddingCompletedDlqMessage dlqMessage = createDlqMessage();

        // Act
        producer.sendToDlq(dlqMessage);

        // Assert
        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<EmbeddingCompletedDlqMessage> capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getPayload()).isEqualTo(dlqMessage);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(DLQ_TOPIC);
    }

    @Test
    void sendToDlq_setsMessageKey() {
        // Arrange
        EmbeddingCompletedDlqMessage dlqMessage = createDlqMessage();

        // Act
        producer.sendToDlq(dlqMessage);

        // Assert
        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<EmbeddingCompletedDlqMessage> capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.KEY)).isEqualTo(INGESTION_JOB_ID);
    }

    @Test
    void sendToDeadLetter_sendsMessageToDeadLetterTopic() {
        // Arrange
        EmbeddingCompletedDlqMessage dlqMessage = createDlqMessage();

        // Act
        producer.sendToDeadLetter(dlqMessage);

        // Assert
        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<EmbeddingCompletedDlqMessage> capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getPayload()).isEqualTo(dlqMessage);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(DEAD_LETTER_TOPIC);
    }

    @Test
    void sendToDeadLetter_setsMessageKey() {
        // Arrange
        EmbeddingCompletedDlqMessage dlqMessage = createDlqMessage();

        // Act
        producer.sendToDeadLetter(dlqMessage);

        // Assert
        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<EmbeddingCompletedDlqMessage> capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.KEY)).isEqualTo(INGESTION_JOB_ID);
    }

    private EmbeddingCompletedDlqMessage createDlqMessage() {
        EmbeddingCompletedEvent originalEvent = new EmbeddingCompletedEvent(
                INGESTION_JOB_ID,
                List.of(),
                "text-embedding-3-small"
        );

        return new EmbeddingCompletedDlqMessage(
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
