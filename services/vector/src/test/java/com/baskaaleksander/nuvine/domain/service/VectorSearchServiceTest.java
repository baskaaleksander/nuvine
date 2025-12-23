package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.infrastructure.client.LlmRouterInternalClient;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.JsonWithInt;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private VectorStorageService storageService;

    @Mock
    private LlmRouterInternalClient llmRouterInternalClient;

    @InjectMocks
    private VectorSearchService vectorSearchService;

    private UUID workspaceId;
    private UUID projectId;
    private UUID documentId;
    private List<UUID> documentIds;
    private List<Float> queryVector;
    private TextVectorSearchRequest textSearchRequest;
    private VectorSearchRequest vectorSearchRequest;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        documentIds = List.of(documentId);
        queryVector = List.of(0.1f, 0.2f, 0.3f);

        textSearchRequest = new TextVectorSearchRequest(
                workspaceId,
                projectId,
                documentIds,
                "test query",
                10,
                0.5f
        );

        vectorSearchRequest = new VectorSearchRequest(
                workspaceId,
                projectId,
                documentIds,
                queryVector,
                10,
                0.5f
        );
    }

    private Points.ScoredPoint createScoredPoint(UUID docId, int page, int startOffset, int endOffset, String content, float score) {
        return Points.ScoredPoint.newBuilder()
                .setScore(score)
                .putPayload("documentId", JsonWithInt.Value.newBuilder().setStringValue(docId.toString()).build())
                .putPayload("page", JsonWithInt.Value.newBuilder().setIntegerValue(page).build())
                .putPayload("startOffset", JsonWithInt.Value.newBuilder().setIntegerValue(startOffset).build())
                .putPayload("endOffset", JsonWithInt.Value.newBuilder().setIntegerValue(endOffset).build())
                .putPayload("content", JsonWithInt.Value.newBuilder().setStringValue(content).build())
                .build();
    }

    @Test
    void searchByText_validRequest_callsLlmRouterForEmbedding() {
        EmbeddingResponse embeddingResponse = new EmbeddingResponse(
                List.of(queryVector),
                "text-embedding-3-small"
        );
        when(llmRouterInternalClient.embed(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);
        when(storageService.search(any(), any(), any(), any(), anyInt(), anyFloat())).thenReturn(List.of());

        vectorSearchService.searchByText(textSearchRequest);

        ArgumentCaptor<EmbeddingRequest> requestCaptor = ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(llmRouterInternalClient).embed(requestCaptor.capture());

        EmbeddingRequest capturedRequest = requestCaptor.getValue();
        assertEquals(1, capturedRequest.texts().size());
        assertEquals("test query", capturedRequest.texts().get(0));
        assertEquals("text-embedding-3-small", capturedRequest.model());
    }

    @Test
    void searchByText_validRequest_delegatesToStorageServiceWithConvertedVector() {
        EmbeddingResponse embeddingResponse = new EmbeddingResponse(
                List.of(queryVector),
                "text-embedding-3-small"
        );
        when(llmRouterInternalClient.embed(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);
        when(storageService.search(any(), any(), any(), any(), anyInt(), anyFloat())).thenReturn(List.of());

        vectorSearchService.searchByText(textSearchRequest);

        verify(storageService).search(
                eq(workspaceId),
                eq(projectId),
                eq(documentIds),
                eq(queryVector),
                eq(10),
                eq(0.5f)
        );
    }

    @Test
    void searchByText_returnsMatchesFromSearch() {
        EmbeddingResponse embeddingResponse = new EmbeddingResponse(
                List.of(queryVector),
                "text-embedding-3-small"
        );
        Points.ScoredPoint point = createScoredPoint(documentId, 1, 100, 200, "test content", 0.95f);
        
        when(llmRouterInternalClient.embed(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);
        when(storageService.search(any(), any(), any(), any(), anyInt(), anyFloat())).thenReturn(List.of(point));

        VectorSearchResponse response = vectorSearchService.searchByText(textSearchRequest);

        assertEquals(1, response.matches().size());
        VectorSearchResponse.VectorSearchMatch match = response.matches().get(0);
        assertEquals(documentId, match.documentId());
        assertEquals(1, match.page());
        assertEquals(100, match.startOffset());
        assertEquals(200, match.endOffset());
        assertEquals("test content", match.content());
        assertEquals(0.95f, match.score(), 0.001f);
    }

    @Test
    void search_validRequest_callsStorageService() {
        when(storageService.search(any(), any(), any(), any(), anyInt(), anyFloat())).thenReturn(List.of());

        vectorSearchService.search(vectorSearchRequest);

        verify(storageService).search(
                eq(workspaceId),
                eq(projectId),
                eq(documentIds),
                eq(queryVector),
                eq(10),
                eq(0.5f)
        );
    }

    @Test
    void search_mapsPointsToVectorSearchMatch() {
        Points.ScoredPoint point = createScoredPoint(documentId, 2, 500, 600, "mapped content", 0.88f);
        when(storageService.search(any(), any(), any(), any(), anyInt(), anyFloat())).thenReturn(List.of(point));

        VectorSearchResponse response = vectorSearchService.search(vectorSearchRequest);

        assertEquals(1, response.matches().size());
        VectorSearchResponse.VectorSearchMatch match = response.matches().get(0);
        assertEquals(documentId, match.documentId());
        assertEquals(2, match.page());
        assertEquals(500, match.startOffset());
        assertEquals(600, match.endOffset());
        assertEquals("mapped content", match.content());
        assertEquals(0.88f, match.score(), 0.001f);
    }

    @Test
    void search_noMatches_returnsEmptyList() {
        when(storageService.search(any(), any(), any(), any(), anyInt(), anyFloat())).thenReturn(List.of());

        VectorSearchResponse response = vectorSearchService.search(vectorSearchRequest);

        assertNotNull(response.matches());
        assertTrue(response.matches().isEmpty());
    }

    @Test
    void search_multipleMatches_returnsAllMatches() {
        UUID docId1 = UUID.randomUUID();
        UUID docId2 = UUID.randomUUID();
        
        Points.ScoredPoint point1 = createScoredPoint(docId1, 1, 0, 100, "content 1", 0.95f);
        Points.ScoredPoint point2 = createScoredPoint(docId2, 2, 100, 200, "content 2", 0.85f);
        
        when(storageService.search(any(), any(), any(), any(), anyInt(), anyFloat()))
                .thenReturn(List.of(point1, point2));

        VectorSearchResponse response = vectorSearchService.search(vectorSearchRequest);

        assertEquals(2, response.matches().size());
        assertEquals(docId1, response.matches().get(0).documentId());
        assertEquals(docId2, response.matches().get(1).documentId());
        assertEquals(0.95f, response.matches().get(0).score(), 0.001f);
        assertEquals(0.85f, response.matches().get(1).score(), 0.001f);
    }

    @Test
    void search_extractsAllPayloadFields() {
        UUID testDocId = UUID.randomUUID();
        Points.ScoredPoint point = createScoredPoint(testDocId, 5, 250, 350, "extracted content", 0.75f);
        
        when(storageService.search(any(), any(), any(), any(), anyInt(), anyFloat())).thenReturn(List.of(point));

        VectorSearchResponse response = vectorSearchService.search(vectorSearchRequest);

        VectorSearchResponse.VectorSearchMatch match = response.matches().get(0);
        assertEquals(testDocId, match.documentId());
        assertEquals(5, match.page());
        assertEquals(250, match.startOffset());
        assertEquals(350, match.endOffset());
        assertEquals("extracted content", match.content());
        assertEquals(0.75f, match.score(), 0.001f);
    }
}
