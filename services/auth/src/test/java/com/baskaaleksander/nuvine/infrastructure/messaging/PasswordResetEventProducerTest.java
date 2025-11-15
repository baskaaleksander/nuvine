package com.baskaaleksander.nuvine.infrastructure.messaging;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetEventProducerTest {

    @Mock
    private KafkaTemplate<String, PasswordResetEvent> kafkaTemplate;

    @InjectMocks
    private PasswordResetEventProducer producer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "passwordResetTopic", "password-reset-topic");
    }

    @Test
    void shouldSendPasswordResetEventWithCorrectTopicAndPayload() {
        String email = "user@example.com";
        String token = "reset-token-123";
        String userId = "user-1";

        PasswordResetEvent event = new PasswordResetEvent(email, token, userId);

        try (MockedStatic<MaskingUtil> mockedMasking = Mockito.mockStatic(MaskingUtil.class)) {
            mockedMasking.when(() -> MaskingUtil.maskEmail(email)).thenReturn("masked-email");

            producer.sendPasswordResetEvent(event);

            ArgumentCaptor<Message<PasswordResetEvent>> messageCaptor =
                    ArgumentCaptor.forClass(Message.class);

            verify(kafkaTemplate, times(1)).send(messageCaptor.capture());

            Message<PasswordResetEvent> sentMessage = messageCaptor.getValue();
            assertNotNull(sentMessage, "Sent message should not be null");
            assertEquals(event, sentMessage.getPayload(), "Payload should be the same PasswordResetEvent instance");
            assertEquals("password-reset-topic",
                    sentMessage.getHeaders().get(KafkaHeaders.TOPIC),
                    "Kafka topic header should match configured password reset topic");

            mockedMasking.verify(() -> MaskingUtil.maskEmail(email), times(1));
        }
    }

    @Test
    void shouldThrowExceptionWhenEventIsNull() {
        PasswordResetEvent event = null;

        assertThrows(IllegalArgumentException.class,
                () -> producer.sendPasswordResetEvent(event),
                "Null event should result in IllegalArgumentException");

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldPropagateExceptionWhenKafkaSendFails() {
        String email = "user@example.com";
        PasswordResetEvent event = new PasswordResetEvent(email, "reset-token-123", "user-1");

        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaTemplate)
                .send(any(Message.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> producer.sendPasswordResetEvent(event),
                "Exception thrown by KafkaTemplate.send should be propagated");

        assertEquals("Kafka error", ex.getMessage());
        verify(kafkaTemplate, times(1)).send(any(Message.class));
    }
}