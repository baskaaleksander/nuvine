package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class LogTokenUsageEventProducerTest {

    @Mock
    private KafkaTemplate<String, LogTokenUsageEvent> kafkaTemplate;

    @InjectMocks
    private LogTokenUsageEventProducer producer;

    private LogTokenUsageEvent event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "topic", "log-token-usage-topic");
        event = new LogTokenUsageEvent(
                "workspace-123",
                "user-456",
                "conversation-789",
                "message-abc",
                "gpt-4",
                "openai",
                "chat-service",
                100L,
                150L,
                Instant.now()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void produceLogTokenUsageEvent_sendsMessageToCorrectTopic() {
        producer.produceLogTokenUsageEvent(event);

        ArgumentCaptor<Message<LogTokenUsageEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<LogTokenUsageEvent> message = messageCaptor.getValue();

        assertEquals(event, message.getPayload());
        assertEquals("log-token-usage-topic", message.getHeaders().get(KafkaHeaders.TOPIC));
        verifyNoMoreInteractions(kafkaTemplate);
    }
}
