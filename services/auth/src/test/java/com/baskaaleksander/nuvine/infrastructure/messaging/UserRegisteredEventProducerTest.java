package com.baskaaleksander.nuvine.infrastructure.messaging;

import com.baskaaleksander.nuvine.application.util.MaskingUtil;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UserRegisteredEvent;
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
class UserRegisteredEventProducerTest {

    @Mock
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @InjectMocks
    private UserRegisteredEventProducer producer;

    private UserRegisteredEvent event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "userRegisteredTopic", "user-registered-topic");

        event = new UserRegisteredEvent(
                "John",
                "Doe",
                "john@example.com",
                "token-123",
                "user-1"
        );
    }

    @Test
    void shouldSendUserRegisteredEventWithCorrectTopicAndPayload() {
        try (MockedStatic<MaskingUtil> mocked = Mockito.mockStatic(MaskingUtil.class)) {
            mocked.when(() -> MaskingUtil.maskEmail(event.email())).thenReturn("masked-email");

            producer.sendUserRegisteredEvent(event);

            ArgumentCaptor<Message<UserRegisteredEvent>> captor = ArgumentCaptor.forClass(Message.class);
            verify(kafkaTemplate).send(captor.capture());

            Message<UserRegisteredEvent> msg = captor.getValue();
            assertNotNull(msg);
            assertEquals(event, msg.getPayload());
            assertEquals("user-registered-topic", msg.getHeaders().get(KafkaHeaders.TOPIC));

            mocked.verify(() -> MaskingUtil.maskEmail(event.email()));
        }
    }

    @Test
    void shouldThrowExceptionWhenEventIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> producer.sendUserRegisteredEvent(null));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldPropagateExceptionWhenKafkaSendFails() {
        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaTemplate)
                .send(any(Message.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> producer.sendUserRegisteredEvent(event));

        assertEquals("Kafka error", ex.getMessage());
        verify(kafkaTemplate).send(any(Message.class));
    }
}