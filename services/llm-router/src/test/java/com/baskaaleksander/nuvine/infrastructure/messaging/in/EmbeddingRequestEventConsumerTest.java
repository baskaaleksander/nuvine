package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmbeddingCompletedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingRequestEventConsumerTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private EmbeddingCompletedEventProducer eventProducer;

    @InjectMocks
    private EmbeddingRequestEventConsumer consumer;

    private UUID documentId;
    private EmbeddingRequestEvent requestEvent;
    private List<Chunk> chunks;
    private List<EmbeddedChunk> embeddedChunks;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        
        Chunk chunk1 = new Chunk(documentId, 1, 0, 100, "content1", 0);
        Chunk chunk2 = new Chunk(documentId, 1, 100, 200, "content2", 1);
        chunks = List.of(chunk1, chunk2);
        
        requestEvent = new EmbeddingRequestEvent(
                "job-123",
                chunks,
                "text-embedding-3-small"
        );

        EmbeddedChunk embedded1 = new EmbeddedChunk(documentId, 1, 0, 100, List.of(0.1f, 0.2f), "content1", 0);
        EmbeddedChunk embedded2 = new EmbeddedChunk(documentId, 1, 100, 200, List.of(0.3f, 0.4f), "content2", 1);
        embeddedChunks = List.of(embedded1, embedded2);
    }

    @Test
    void consumeEmbeddingRequestEvent_callsEmbeddingService() {
        when(embeddingService.createEmbeddings(chunks)).thenReturn(embeddedChunks);

        consumer.consumeEmbeddingRequestEvent(requestEvent);

        verify(embeddingService).createEmbeddings(chunks);
    }

    @Test
    void consumeEmbeddingRequestEvent_publishesCompletionEvent() {
        when(embeddingService.createEmbeddings(chunks)).thenReturn(embeddedChunks);

        consumer.consumeEmbeddingRequestEvent(requestEvent);

        verify(eventProducer).sendEmbeddingCompletedEvent(any(EmbeddingCompletedEvent.class));
    }

    @Test
    void consumeEmbeddingRequestEvent_passesCorrectJobId() {
        when(embeddingService.createEmbeddings(chunks)).thenReturn(embeddedChunks);

        consumer.consumeEmbeddingRequestEvent(requestEvent);

        ArgumentCaptor<EmbeddingCompletedEvent> captor = ArgumentCaptor.forClass(EmbeddingCompletedEvent.class);
        verify(eventProducer).sendEmbeddingCompletedEvent(captor.capture());

        EmbeddingCompletedEvent completedEvent = captor.getValue();
        assertEquals("job-123", completedEvent.ingestionJobId());
        assertEquals("text-embedding-3-small", completedEvent.model());
        assertEquals(2, completedEvent.embeddedChunks().size());
        assertEquals(embeddedChunks, completedEvent.embeddedChunks());
    }
}
