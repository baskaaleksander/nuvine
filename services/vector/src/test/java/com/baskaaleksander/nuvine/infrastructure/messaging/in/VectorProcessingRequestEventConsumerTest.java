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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorProcessingRequestEventConsumerTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private VectorProcessingRequestDlqProducer vectorProcessingRequestDlqProducer;

    @InjectMocks
    private VectorProcessingRequestEventConsumer consumer;

    private VectorProcessingRequestEvent event;
    private String topicName;

    @BeforeEach
    void setUp() {
        topicName = "vector-processing-request-topic";
        ReflectionTestUtils.setField(consumer, "vectorProcessingRequestTopic", topicName);

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
    }

    @Test
    void consume_validEvent_callsEmbeddingService() {
        consumer.consumeVectorProcessingRequestEvent(event);

        verify(embeddingService).process(event);
        verifyNoInteractions(vectorProcessingRequestDlqProducer);
    }

    @Test
    void consume_processingError_sendsToDlq() {
        doThrow(new RuntimeException("Processing failed")).when(embeddingService).process(event);

        consumer.consumeVectorProcessingRequestEvent(event);

        verify(vectorProcessingRequestDlqProducer).sendToDlq(any(VectorProcessingRequestDlqMessage.class));
    }

    @Test
    void consume_error_createsDlqMessageWithCorrectFields() {
        RuntimeException exception = new RuntimeException("Test error message");
        doThrow(exception).when(embeddingService).process(event);

        consumer.consumeVectorProcessingRequestEvent(event);

        ArgumentCaptor<VectorProcessingRequestDlqMessage> dlqCaptor = 
                ArgumentCaptor.forClass(VectorProcessingRequestDlqMessage.class);
        verify(vectorProcessingRequestDlqProducer).sendToDlq(dlqCaptor.capture());

        VectorProcessingRequestDlqMessage dlqMessage = dlqCaptor.getValue();
        assertEquals(event, dlqMessage.originalEvent());
        assertEquals("Test error message", dlqMessage.errorMessage());
        assertEquals(topicName, dlqMessage.originalTopic());
        assertEquals(1, dlqMessage.attemptCount());
        assertNotNull(dlqMessage.firstFailedAt());
        assertNotNull(dlqMessage.lastFailedAt());
    }
}
