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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingCompletedDlqWorkerTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private EmbeddingCompletedDlqProducer dlqProducer;

    @InjectMocks
    private EmbeddingCompletedDlqWorker worker;

    private EmbeddingCompletedEvent event;
    private EmbeddingCompletedDlqMessage dlqMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(worker, "maxRetryAttempts", 10);

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

        dlqMessage = new EmbeddingCompletedDlqMessage(
                event,
                1,
                "Initial error",
                "java.lang.RuntimeException",
                Instant.now(),
                Instant.now(),
                "embedding-completed-topic"
        );
    }

    @Test
    void processDlqBatch_validMessages_processesAll() {
        EmbeddingCompletedDlqMessage message2 = new EmbeddingCompletedDlqMessage(
                event, 2, "Error 2", "RuntimeException", Instant.now(), Instant.now(), "topic"
        );

        worker.processDlqBatch(List.of(dlqMessage, message2));

        verify(embeddingService, times(2)).processEmbeddingCompletedEvent(event);
    }

    @Test
    void processMessage_success_noFurtherAction() {
        worker.processDlqBatch(List.of(dlqMessage));

        verify(embeddingService).processEmbeddingCompletedEvent(event);
        verifyNoInteractions(dlqProducer);
    }

    @Test
    void processMessage_transientFailure_incrementsAttemptAndRequeues() {
        doThrow(new RuntimeException("Transient error")).when(embeddingService).processEmbeddingCompletedEvent(event);

        worker.processDlqBatch(List.of(dlqMessage));

        ArgumentCaptor<EmbeddingCompletedDlqMessage> captor = 
                ArgumentCaptor.forClass(EmbeddingCompletedDlqMessage.class);
        verify(dlqProducer).sendToDlq(captor.capture());

        EmbeddingCompletedDlqMessage sentMessage = captor.getValue();
        assertEquals(2, sentMessage.attemptCount());
        assertEquals("Transient error", sentMessage.errorMessage());
    }

    @Test
    void processMessage_maxRetriesExceeded_movesToDeadLetter() {
        EmbeddingCompletedDlqMessage messageAtMax = new EmbeddingCompletedDlqMessage(
                event, 10, "Error", "RuntimeException", Instant.now(), Instant.now(), "topic"
        );
        doThrow(new RuntimeException("Still failing")).when(embeddingService).processEmbeddingCompletedEvent(event);

        worker.processDlqBatch(List.of(messageAtMax));

        verify(dlqProducer).sendToDeadLetter(any(EmbeddingCompletedDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processMessage_permanentFailure_jobNotFound_movesToDeadLetterImmediately() {
        doThrow(new RuntimeException("Job not found")).when(embeddingService).processEmbeddingCompletedEvent(event);

        worker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any(EmbeddingCompletedDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processMessage_permanentFailure_notFound_movesToDeadLetterImmediately() {
        doThrow(new RuntimeException("Entity not found")).when(embeddingService).processEmbeddingCompletedEvent(event);

        worker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any(EmbeddingCompletedDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processMessage_permanentFailure_illegalArgument_movesToDeadLetterImmediately() {
        doThrow(new IllegalArgumentException("Invalid argument")).when(embeddingService).processEmbeddingCompletedEvent(event);

        worker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any(EmbeddingCompletedDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }
}
