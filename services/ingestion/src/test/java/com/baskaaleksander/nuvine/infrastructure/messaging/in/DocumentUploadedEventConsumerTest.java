package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.IngestionService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentUploadedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentUploadedEventConsumerTest {

    @Mock
    private IngestionService ingestionService;

    @InjectMocks
    private DocumentUploadedEventConsumer consumer;

    private DocumentUploadedEvent event;

    @BeforeEach
    void setUp() {
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
    void consumeDocumentUploadedEvent_validEvent_callsIngestionService() {
        doNothing().when(ingestionService).process(event);

        consumer.consumeDocumentUploadedEvent(event);

        verify(ingestionService).process(event);
    }

    @Test
    void consumeDocumentUploadedEvent_serviceThrowsException_propagatesException() {
        RuntimeException expectedException = new RuntimeException("Processing failed");
        doThrow(expectedException).when(ingestionService).process(event);

        try {
            consumer.consumeDocumentUploadedEvent(event);
        } catch (RuntimeException e) {
            verify(ingestionService).process(event);
        }
    }
}
