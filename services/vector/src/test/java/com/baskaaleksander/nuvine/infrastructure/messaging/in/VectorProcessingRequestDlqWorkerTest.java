package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingRequestDlqProducer;
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
class VectorProcessingRequestDlqWorkerTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorProcessingRequestDlqProducer dlqProducer;

    @InjectMocks
    private VectorProcessingRequestDlqWorker worker;

    private VectorProcessingRequestEvent event;
    private VectorProcessingRequestDlqMessage dlqMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(worker, "maxRetryAttempts", 10);

        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID ingestionJobId = UUID.randomUUID();

        List<Chunk> chunks = List.of(
                new Chunk(documentId, 0, 0, 100, "Test content", 0)
        );

        event = new VectorProcessingRequestEvent(
                ingestionJobId.toString(),
                documentId.toString(),
                projectId.toString(),
                workspaceId.toString(),
                chunks
        );

        dlqMessage = new VectorProcessingRequestDlqMessage(
                event,
                1,
                "Initial error",
                "java.lang.RuntimeException",
                Instant.now(),
                Instant.now(),
                "vector-processing-request-topic"
        );
    }

    @Test
    void processDlqBatch_validMessages_processesAll() {
        VectorProcessingRequestDlqMessage message2 = new VectorProcessingRequestDlqMessage(
                event, 2, "Error 2", "RuntimeException", Instant.now(), Instant.now(), "topic"
        );

        worker.processDlqBatch(List.of(dlqMessage, message2));

        verify(embeddingService, times(2)).process(event);
    }

    @Test
    void processMessage_success_noFurtherAction() {
        worker.processDlqBatch(List.of(dlqMessage));

        verify(embeddingService).process(event);
        verifyNoInteractions(dlqProducer);
    }

    @Test
    void processMessage_transientFailure_incrementsAttemptAndRequeues() {
        doThrow(new RuntimeException("Transient error")).when(embeddingService).process(event);

        worker.processDlqBatch(List.of(dlqMessage));

        ArgumentCaptor<VectorProcessingRequestDlqMessage> captor = 
                ArgumentCaptor.forClass(VectorProcessingRequestDlqMessage.class);
        verify(dlqProducer).sendToDlq(captor.capture());

        VectorProcessingRequestDlqMessage sentMessage = captor.getValue();
        assertEquals(2, sentMessage.attemptCount());
        assertEquals("Transient error", sentMessage.errorMessage());
    }

    @Test
    void processMessage_maxRetriesExceeded_movesToDeadLetter() {
        VectorProcessingRequestDlqMessage messageAtMax = new VectorProcessingRequestDlqMessage(
                event, 10, "Error", "RuntimeException", Instant.now(), Instant.now(), "topic"
        );
        doThrow(new RuntimeException("Still failing")).when(embeddingService).process(event);

        worker.processDlqBatch(List.of(messageAtMax));

        verify(dlqProducer).sendToDeadLetter(any(VectorProcessingRequestDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processMessage_permanentFailure_illegalArgument_movesToDeadLetterImmediately() {
        doThrow(new IllegalArgumentException("Invalid UUID")).when(embeddingService).process(event);

        worker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any(VectorProcessingRequestDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processMessage_permanentFailure_nullPointer_movesToDeadLetterImmediately() {
        doThrow(new NullPointerException("Required field is null")).when(embeddingService).process(event);

        worker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any(VectorProcessingRequestDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processMessage_permanentFailure_invalidUuid_movesToDeadLetterImmediately() {
        doThrow(new RuntimeException("Invalid UUID string")).when(embeddingService).process(event);

        worker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any(VectorProcessingRequestDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processMessage_permanentFailure_cannotBeNull_movesToDeadLetterImmediately() {
        doThrow(new RuntimeException("Field cannot be null")).when(embeddingService).process(event);

        worker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any(VectorProcessingRequestDlqMessage.class));
        verify(dlqProducer, never()).sendToDlq(any());
    }
}
