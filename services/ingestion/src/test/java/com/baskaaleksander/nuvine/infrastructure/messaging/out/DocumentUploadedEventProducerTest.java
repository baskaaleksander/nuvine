package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
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
class DocumentUploadedEventProducerTest {

    @Mock
    private KafkaTemplate<String, DocumentUploadedEvent> kafkaTemplate;

    @InjectMocks
    private DocumentUploadedEventProducer producer;

    @Captor
    private ArgumentCaptor<Message<DocumentUploadedEvent>> messageCaptor;

    private DocumentUploadedEvent event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "topic", "document-uploaded");

        event = new DocumentUploadedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "workspaces/test/documents/test.pdf",
                "application/pdf",
                1024L
        );
    }

    @Test
    void sendDocumentUploadedEvent_validEvent_sendsToKafka() {
        producer.sendDocumentUploadedEvent(event);

        verify(kafkaTemplate).send(messageCaptor.capture());
        
        Message<DocumentUploadedEvent> capturedMessage = messageCaptor.getValue();
        assertEquals(event, capturedMessage.getPayload());
        assertEquals("document-uploaded", capturedMessage.getHeaders().get("kafka_topic"));
    }

    @Test
    void sendDocumentUploadedEvent_multipleEvents_sendsAll() {
        DocumentUploadedEvent event2 = new DocumentUploadedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "workspaces/test2/documents/test2.pdf",
                "application/pdf",
                2048L
        );

        producer.sendDocumentUploadedEvent(event);
        producer.sendDocumentUploadedEvent(event2);

        verify(kafkaTemplate, times(2)).send(any(Message.class));
    }
}
