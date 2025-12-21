package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionCompletedEvent;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionCompletedEventProducerTest {

    @Mock
    private KafkaTemplate<String, DocumentIngestionCompletedEvent> kafkaTemplate;

    @InjectMocks
    private DocumentIngestionCompletedEventProducer producer;

    @Captor
    private ArgumentCaptor<Message<DocumentIngestionCompletedEvent>> messageCaptor;

    private DocumentIngestionCompletedEvent event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "topic", "document-ingestion-completed");

        event = new DocumentIngestionCompletedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
    }

    @Test
    void sendDocumentIngestionCompletedEvent_validEvent_sendsToKafka() {
        producer.sendDocumentIngestionCompletedEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());
        
        Message<DocumentIngestionCompletedEvent> capturedMessage = messageCaptor.getValue();
        assertEquals(event, capturedMessage.getPayload());
        assertEquals("document-ingestion-completed", capturedMessage.getHeaders().get("kafka_topic"));
    }
}
