package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmbeddingCompletedDlqProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingCompletedEventConsumerTest {

    @Mock
    private EmbeddingService service;

    @Mock
    private EmbeddingCompletedDlqProducer dlqProducer;

    @InjectMocks
    private EmbeddingCompletedEventConsumer consumer;

    private EmbeddingCompletedEvent event;
    private String topicName;

    @BeforeEach
    void setUp() {
        topicName = "embedding-completed-topic";
        ReflectionTestUtils.setField(consumer, "embeddingCompletedTopic", topicName);

        UUID documentId = UUID.randomUUID();
        UUID ingestionJobId = UUID.randomUUID();

        List<EmbeddedChunk> embeddedChunks = List.of(
                new EmbeddedChunk(documentId, 0, 0, 100, List.of(0.1f, 0.2f), "content", 0)
        );

        event = new EmbeddingCompletedEvent(
                ingestionJobId.toString(),
                embeddedChunks,
                "text-embedding-3-small"
        );
    }

    @Test
    void consume_validEvent_callsEmbeddingService() {
        consumer.consumeEmbeddingCompletedEvent(event);

        verify(service).processEmbeddingCompletedEvent(event);
        verifyNoInteractions(dlqProducer);
    }

    @Test
    void consume_transientError_sendsToDlq() {
        doThrow(new RuntimeException("Transient error")).when(service).processEmbeddingCompletedEvent(event);

        consumer.consumeEmbeddingCompletedEvent(event);

        verify(dlqProducer).sendToDlq(any(EmbeddingCompletedDlqMessage.class));
        verify(dlqProducer, never()).sendToDeadLetter(any());
    }

    @Test
    void consume_permanentFailure_jobNotFound_sendsToDeadLetter() {
        doThrow(new RuntimeException("Job not found")).when(service).processEmbeddingCompletedEvent(event);

        consumer.consumeEmbeddingCompletedEvent(event);

        verify(dlqProducer).sendToDeadLetter(any(EmbeddingCompletedDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void consume_permanentFailure_notFound_sendsToDeadLetter() {
        doThrow(new RuntimeException("Entity not found in database")).when(service).processEmbeddingCompletedEvent(event);

        consumer.consumeEmbeddingCompletedEvent(event);

        verify(dlqProducer).sendToDeadLetter(any(EmbeddingCompletedDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void consume_permanentFailure_illegalArgument_sendsToDeadLetter() {
        doThrow(new IllegalArgumentException("Invalid argument")).when(service).processEmbeddingCompletedEvent(event);

        consumer.consumeEmbeddingCompletedEvent(event);

        verify(dlqProducer).sendToDeadLetter(any(EmbeddingCompletedDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void consume_error_createsDlqMessageWithCorrectFields() {
        RuntimeException exception = new RuntimeException("Test transient error");
        doThrow(exception).when(service).processEmbeddingCompletedEvent(event);

        consumer.consumeEmbeddingCompletedEvent(event);

        ArgumentCaptor<EmbeddingCompletedDlqMessage> dlqCaptor = 
                ArgumentCaptor.forClass(EmbeddingCompletedDlqMessage.class);
        verify(dlqProducer).sendToDlq(dlqCaptor.capture());

        EmbeddingCompletedDlqMessage dlqMessage = dlqCaptor.getValue();
        assertEquals(event, dlqMessage.originalEvent());
        assertEquals("Test transient error", dlqMessage.errorMessage());
        assertEquals(topicName, dlqMessage.originalTopic());
        assertEquals(1, dlqMessage.attemptCount());
        assertNotNull(dlqMessage.firstFailedAt());
    }
}
