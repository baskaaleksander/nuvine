package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.EmbeddingRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingResponse;
import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.infrastructure.ai.client.OpenAIEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private OpenAIEmbeddingClient embeddingClient;

    @InjectMocks
    private EmbeddingService embeddingService;

    private UUID documentId;
    private List<Float> embedding1;
    private List<Float> embedding2;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        embedding1 = List.of(0.1f, 0.2f, 0.3f);
        embedding2 = List.of(0.4f, 0.5f, 0.6f);
    }

    @Test
    void createEmbeddings_validChunks_returnsEmbeddedChunks() {
        Chunk chunk1 = new Chunk(documentId, 1, 0, 100, "First chunk content", 0);
        Chunk chunk2 = new Chunk(documentId, 1, 100, 200, "Second chunk content", 1);
        List<Chunk> chunks = List.of(chunk1, chunk2);

        when(embeddingClient.embed(List.of("First chunk content", "Second chunk content")))
                .thenReturn(List.of(embedding1, embedding2));

        List<EmbeddedChunk> result = embeddingService.createEmbeddings(chunks);

        assertEquals(2, result.size());
        assertEquals(embedding1, result.get(0).embedding());
        assertEquals(embedding2, result.get(1).embedding());
        verify(embeddingClient).embed(List.of("First chunk content", "Second chunk content"));
    }

    @Test
    void createEmbeddings_preservesChunkMetadata() {
        Chunk chunk = new Chunk(documentId, 5, 50, 150, "Test content", 0);
        List<Chunk> chunks = List.of(chunk);

        when(embeddingClient.embed(List.of("Test content")))
                .thenReturn(List.of(embedding1));

        List<EmbeddedChunk> result = embeddingService.createEmbeddings(chunks);

        assertEquals(1, result.size());
        EmbeddedChunk embeddedChunk = result.get(0);
        assertEquals(documentId, embeddedChunk.documentId());
        assertEquals(5, embeddedChunk.page());
        assertEquals(50, embeddedChunk.startOffset());
        assertEquals(150, embeddedChunk.endOffset());
        assertEquals("Test content", embeddedChunk.content());
        assertEquals(0, embeddedChunk.index());
        assertEquals(embedding1, embeddedChunk.embedding());
    }

    @Test
    void createEmbeddings_validRequest_returnsEmbeddingResponse() {
        EmbeddingRequest request = new EmbeddingRequest(
                List.of("text1", "text2"),
                "text-embedding-3-small"
        );

        when(embeddingClient.embed(List.of("text1", "text2")))
                .thenReturn(List.of(embedding1, embedding2));

        EmbeddingResponse response = embeddingService.createEmbeddings(request);

        assertEquals(2, response.embeddings().size());
        assertEquals(embedding1, response.embeddings().get(0));
        assertEquals(embedding2, response.embeddings().get(1));
        assertEquals("text-embedding-3-small", response.usedModel());
        verify(embeddingClient).embed(List.of("text1", "text2"));
    }

    @Test
    void createEmbeddings_embeddingClientError_throwsRuntimeException() {
        Chunk chunk = new Chunk(documentId, 1, 0, 100, "Test content", 0);
        List<Chunk> chunks = List.of(chunk);

        when(embeddingClient.embed(anyList()))
                .thenThrow(new RuntimeException("API error"));

        assertThrows(RuntimeException.class, () -> embeddingService.createEmbeddings(chunks));
        verify(embeddingClient).embed(List.of("Test content"));
    }
}
