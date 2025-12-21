package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.TextVectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchResponse;
import com.baskaaleksander.nuvine.infrastructure.client.VectorServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextRetrievalServiceTest {

    @Mock
    private VectorServiceClient vectorServiceClient;

    @InjectMocks
    private ContextRetrievalService contextRetrievalService;

    private UUID workspaceId;
    private UUID projectId;
    private List<UUID> documentIds;
    private String query;
    private int topK;
    private float threshold;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        documentIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        query = "What is the meaning of life?";
        topK = 10;
        threshold = 0.5f;
    }


    @Test
    void retrieveContext_validQuery_returnsContentStrings() {
        VectorSearchResponse.VectorSearchMatch match1 = new VectorSearchResponse.VectorSearchMatch(
                documentIds.get(0), 1, 0, 100, "First chunk of relevant content", 0.85f
        );
        VectorSearchResponse.VectorSearchMatch match2 = new VectorSearchResponse.VectorSearchMatch(
                documentIds.get(1), 2, 50, 150, "Second chunk of relevant content", 0.75f
        );
        VectorSearchResponse response = new VectorSearchResponse(List.of(match1, match2));

        when(vectorServiceClient.searchText(any(TextVectorSearchRequest.class))).thenReturn(response);

        List<String> result = contextRetrievalService.retrieveContext(
                workspaceId, projectId, documentIds, query, topK, threshold
        );

        assertEquals(2, result.size());
        assertEquals("First chunk of relevant content", result.get(0));
        assertEquals("Second chunk of relevant content", result.get(1));
        verify(vectorServiceClient).searchText(any(TextVectorSearchRequest.class));
    }

    @Test
    void retrieveContext_noMatches_returnsEmptyList() {
        VectorSearchResponse response = new VectorSearchResponse(List.of());

        when(vectorServiceClient.searchText(any(TextVectorSearchRequest.class))).thenReturn(response);

        List<String> result = contextRetrievalService.retrieveContext(
                workspaceId, projectId, documentIds, query, topK, threshold
        );

        assertTrue(result.isEmpty());
        verify(vectorServiceClient).searchText(any(TextVectorSearchRequest.class));
    }

    @Test
    void retrieveContext_clientError_propagatesException() {
        when(vectorServiceClient.searchText(any(TextVectorSearchRequest.class)))
                .thenThrow(new RuntimeException("Vector service unavailable"));

        assertThrows(RuntimeException.class, () ->
                contextRetrievalService.retrieveContext(
                        workspaceId, projectId, documentIds, query, topK, threshold
                )
        );

        verify(vectorServiceClient).searchText(any(TextVectorSearchRequest.class));
    }
}
