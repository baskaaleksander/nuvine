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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorProcessingCompletedDlqWorkerTest {

    @Mock
    private IngestionStatusOrchestrator ingestionStatusOrchestrator;

    @Mock
    private VectorProcessingCompletedDlqProducer dlqProducer;

    @InjectMocks
    private VectorProcessingCompletedDlqWorker dlqWorker;

    @Captor
    private ArgumentCaptor<VectorProcessingCompletedDlqMessage> dlqMessageCaptor;

    private VectorProcessingCompletedEvent event;
    private VectorProcessingCompletedDlqMessage dlqMessage;
    private String ingestionJobId;
    private String documentId;
    private String projectId;
    private String workspaceId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dlqWorker, "maxRetryAttempts", 10);

        ingestionJobId = UUID.randomUUID().toString();
        documentId = UUID.randomUUID().toString();
        projectId = UUID.randomUUID().toString();
        workspaceId = UUID.randomUUID().toString();

        event = new VectorProcessingCompletedEvent(
                ingestionJobId,
                documentId,
                projectId,
                workspaceId
        );

        dlqMessage = new VectorProcessingCompletedDlqMessage(
                event,
                1,
                "Test error",
                "java.lang.RuntimeException",
                Instant.now().minusSeconds(60),
                Instant.now(),
                "vector-processing-completed"
        );
    }

    @Test
    void processDlqBatch_successfulProcessing_completesWithoutRetry() {
        doNothing().when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(dlqMessage));

        verify(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);
        verify(dlqProducer, never()).sendToDlq(any());
        verify(dlqProducer, never()).sendToDeadLetter(any());
    }

    @Test
    void processDlqBatch_multipleMessages_processesAll() {
        String ingestionJobId2 = UUID.randomUUID().toString();
        VectorProcessingCompletedEvent event2 = new VectorProcessingCompletedEvent(
                ingestionJobId2,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        VectorProcessingCompletedDlqMessage dlqMessage2 = new VectorProcessingCompletedDlqMessage(
                event2, 1, "Error", "Exception", Instant.now(), Instant.now(), "topic"
        );

        doNothing().when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(anyString());

        dlqWorker.processDlqBatch(List.of(dlqMessage, dlqMessage2));

        verify(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);
        verify(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId2);
    }

    @Test
    void processDlqBatch_transientFailure_schedulesRetry() {
        RuntimeException transientError = new RuntimeException("Database connection failed");
        doThrow(transientError).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDlq(dlqMessageCaptor.capture());
        verify(dlqProducer, never()).sendToDeadLetter(any());

        VectorProcessingCompletedDlqMessage capturedMessage = dlqMessageCaptor.getValue();
        assertEquals(2, capturedMessage.attemptCount()); // Incremented from 1
        assertEquals(event, capturedMessage.originalEvent());
    }

    @Test
    void processDlqBatch_maxRetriesExceeded_sendsToDeadLetter() {
        VectorProcessingCompletedDlqMessage maxRetryMessage = new VectorProcessingCompletedDlqMessage(
                event,
                9, // At 9, next attempt becomes 10, which equals maxRetryAttempts
                "Error",
                "RuntimeException",
                Instant.now().minusSeconds(3600),
                Instant.now(),
                "topic"
        );

        RuntimeException error = new RuntimeException("Still failing");
        doThrow(error).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(maxRetryMessage));

        verify(dlqProducer).sendToDeadLetter(dlqMessageCaptor.capture());
        verify(dlqProducer, never()).sendToDlq(any());

        VectorProcessingCompletedDlqMessage capturedMessage = dlqMessageCaptor.getValue();
        assertEquals(10, capturedMessage.attemptCount());
    }

    @Test
    void processDlqBatch_permanentFailure_illegalArgumentException_sendsToDeadLetter() {
        IllegalArgumentException permanentError = new IllegalArgumentException("Invalid UUID format");
        doThrow(permanentError).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any());
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processDlqBatch_permanentFailure_nullPointerException_sendsToDeadLetter() {
        NullPointerException permanentError = new NullPointerException("Required field is null");
        doThrow(permanentError).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any());
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processDlqBatch_permanentFailure_invalidUuidMessage_sendsToDeadLetter() {
        RuntimeException permanentError = new RuntimeException("Invalid UUID string: not-a-uuid");
        doThrow(permanentError).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any());
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processDlqBatch_permanentFailure_cannotBeNullMessage_sendsToDeadLetter() {
        RuntimeException permanentError = new RuntimeException("Field documentId cannot be null");
        doThrow(permanentError).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any());
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processDlqBatch_permanentFailure_mustNotBeNullMessage_sendsToDeadLetter() {
        RuntimeException permanentError = new RuntimeException("workspaceId must not be null");
        doThrow(permanentError).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDeadLetter(any());
        verify(dlqProducer, never()).sendToDlq(any());
    }

    @Test
    void processDlqBatch_errorWithNullMessage_treatedAsTransient() {
        RuntimeException errorWithNullMessage = new RuntimeException((String) null);
        doThrow(errorWithNullMessage).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDlq(any());
        verify(dlqProducer, never()).sendToDeadLetter(any());
    }

    @Test
    void processDlqBatch_emptyBatch_noProcessing() {
        dlqWorker.processDlqBatch(List.of());

        verify(ingestionStatusOrchestrator, never()).handleVectorProcessingCompleted(anyString());
        verify(dlqProducer, never()).sendToDlq(any());
        verify(dlqProducer, never()).sendToDeadLetter(any());
    }

    @Test
    void processDlqBatch_retryPreservesOriginalEvent() {
        RuntimeException transientError = new RuntimeException("Temporary failure");
        doThrow(transientError).when(ingestionStatusOrchestrator).handleVectorProcessingCompleted(ingestionJobId);

        dlqWorker.processDlqBatch(List.of(dlqMessage));

        verify(dlqProducer).sendToDlq(dlqMessageCaptor.capture());
        
        VectorProcessingCompletedDlqMessage capturedMessage = dlqMessageCaptor.getValue();
        // Original event should be preserved
        assertEquals(event.ingestionJobId(), capturedMessage.originalEvent().ingestionJobId());
        assertEquals(event.documentId(), capturedMessage.originalEvent().documentId());
        assertEquals(event.projectId(), capturedMessage.originalEvent().projectId());
        assertEquals(event.workspaceId(), capturedMessage.originalEvent().workspaceId());
        // First failed at should be preserved
        assertEquals(dlqMessage.firstFailedAt(), capturedMessage.firstFailedAt());
    }
}
