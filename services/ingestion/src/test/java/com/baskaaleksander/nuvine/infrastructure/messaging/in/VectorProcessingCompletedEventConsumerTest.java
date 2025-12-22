package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.IngestionStatusOrchestrator;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingCompletedDlqProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorProcessingCompletedEventConsumerTest {

    @Mock
    private IngestionStatusOrchestrator ingestionStatusOrchestrator;

    @Mock
    private VectorProcessingCompletedDlqProducer vectorProcessingCompletedDlqProducer;

    @InjectMocks
    private VectorProcessingCompletedEventConsumer consumer;

    @Captor
    private ArgumentCaptor<VectorProcessingCompletedDlqMessage> dlqMessageCaptor;

    private VectorProcessingCompletedEvent event;
    private String ingestionJobId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consumer, "vectorProcessingCompletedTopic", "vector-processing-completed");

        ingestionJobId = UUID.randomUUID().toString();
        event = new VectorProcessingCompletedEvent(
                ingestionJobId,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
    }

    @Test
    void consumeVectorProcessingCompletedEvent_success_completesNormally() {
        doNothing().when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        consumer.consumeVectorProcessingCompletedEvent(event);

        verify(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);
        verify(vectorProcessingCompletedDlqProducer, never()).sendToDlq(any());
    }

    @Test
    void consumeVectorProcessingCompletedEvent_failure_sendsToDlq() {
        RuntimeException error = new RuntimeException("Processing failed");
        doThrow(error).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        consumer.consumeVectorProcessingCompletedEvent(event);

        verify(vectorProcessingCompletedDlqProducer).sendToDlq(dlqMessageCaptor.capture());
        
        VectorProcessingCompletedDlqMessage capturedMessage = dlqMessageCaptor.getValue();
        assertEquals(event, capturedMessage.originalEvent());
        assertEquals(1, capturedMessage.attemptCount());
        assertEquals("Processing failed", capturedMessage.errorMessage());
        assertEquals("vector-processing-completed", capturedMessage.originalTopic());
    }
}
