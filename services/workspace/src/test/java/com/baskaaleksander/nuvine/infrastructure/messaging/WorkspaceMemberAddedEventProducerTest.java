package com.baskaaleksander.nuvine.infrastructure.messaging;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberAddedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceMemberAddedEventProducer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class WorkspaceMemberAddedEventProducerTest {

    @Mock
    private KafkaTemplate<String, WorkspaceMemberAddedEvent> kafkaTemplate;

    @InjectMocks
    private WorkspaceMemberAddedEventProducer producer;

    private WorkspaceMemberAddedEvent event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "topic", "workspace-member-added-topic");
        event = new WorkspaceMemberAddedEvent("user@example.com", "user-id", "workspace-id", "VIEWER");
    }

    @Test
    void sendWorkspaceMemberAddedEvent_sendsMessageWithTopicAndPayload() {
        producer.sendWorkspaceMemberAddedEvent(event);

        ArgumentCaptor<Message<WorkspaceMemberAddedEvent>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<WorkspaceMemberAddedEvent> message = messageCaptor.getValue();

        assertEquals(event, message.getPayload());
        assertEquals("workspace-member-added-topic", message.getHeaders().get(KafkaHeaders.TOPIC));
        verifyNoMoreInteractions(kafkaTemplate);
    }
}
