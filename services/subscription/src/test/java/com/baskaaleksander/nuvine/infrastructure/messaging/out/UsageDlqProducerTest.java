package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsageDlqProducer")
class UsageDlqProducerTest {

    @Mock
    private KafkaTemplate<String, DlqMessage> kafkaTemplate;

    @InjectMocks
    private UsageDlqProducer producer;

    @Captor
    private ArgumentCaptor<Message<DlqMessage>> messageCaptor;

    private static final String DLQ_TOPIC = "usage-logs-dlq-topic";
    private static final String DEAD_LETTER_TOPIC = "usage-logs-dead-letter-topic";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "dlqTopic", DLQ_TOPIC);
        ReflectionTestUtils.setField(producer, "deadLetterTopic", DEAD_LETTER_TOPIC);
    }

    @Test
    @DisplayName("sendToDlq sends message to DLQ topic with correct key")
    void sendToDlq_sendsToCorrectTopic() {
        DlqMessage dlqMessage = TestFixtures.dlqMessage();

        producer.sendToDlq(dlqMessage);

        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<DlqMessage> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getPayload()).isEqualTo(dlqMessage);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(DLQ_TOPIC);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.KEY))
                .isEqualTo(dlqMessage.originalEvent().workspaceId());
    }

    @Test
    @DisplayName("sendToDeadLetter sends message to dead letter topic with correct key")
    void sendToDeadLetter_sendsToCorrectTopic() {
        DlqMessage dlqMessage = TestFixtures.dlqMessage(10);

        producer.sendToDeadLetter(dlqMessage);

        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<DlqMessage> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getPayload()).isEqualTo(dlqMessage);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(DEAD_LETTER_TOPIC);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.KEY))
                .isEqualTo(dlqMessage.originalEvent().workspaceId());
    }

    @Test
    @DisplayName("sendToDlq preserves original event and error details")
    void sendToDlq_preservesEventAndErrorDetails() {
        DlqMessage dlqMessage = TestFixtures.dlqMessage();

        producer.sendToDlq(dlqMessage);

        verify(kafkaTemplate).send(messageCaptor.capture());

        DlqMessage payload = messageCaptor.getValue().getPayload();
        assertThat(payload.originalEvent()).isEqualTo(dlqMessage.originalEvent());
        assertThat(payload.attemptCount()).isEqualTo(dlqMessage.attemptCount());
        assertThat(payload.errorMessage()).isEqualTo(dlqMessage.errorMessage());
        assertThat(payload.errorClass()).isEqualTo(dlqMessage.errorClass());
        assertThat(payload.originalTopic()).isEqualTo(dlqMessage.originalTopic());
    }
}
