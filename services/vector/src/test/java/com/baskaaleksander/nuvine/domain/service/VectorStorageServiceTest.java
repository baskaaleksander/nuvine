package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.ChunkMetadata;
import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.infrastructure.config.QdrantConfig;
import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorStorageServiceTest {

    @Mock
    private QdrantClient qdrantClient;

    @Mock
    private QdrantConfig.QdrantProperties props;

    @InjectMocks
    private VectorStorageService vectorStorageService;

    private UUID workspaceId;
    private UUID projectId;
    private UUID documentId;
    private ChunkMetadata metadata;
    private List<EmbeddedChunk> embeddedChunks;
    private String collectionName;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        collectionName = "test-collection";

        metadata = new ChunkMetadata(workspaceId, projectId);
        embeddedChunks = createEmbeddedChunks(3);
    }

    private List<EmbeddedChunk> createEmbeddedChunks(int count) {
        List<EmbeddedChunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(new EmbeddedChunk(
                    documentId,
                    i,
                    i * 100,
                    (i + 1) * 100,
                    List.of(0.1f, 0.2f, 0.3f),
                    "Content " + i,
                    i
            ));
        }
        return chunks;
    }

    @SuppressWarnings("unchecked")
    @Test
    void upsert_validChunks_callsQdrantWithCorrectCollection() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<Points.UpdateResult> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(Points.UpdateResult.getDefaultInstance());
        when(qdrantClient.upsertAsync(eq(collectionName), any(List.class))).thenReturn(future);

        vectorStorageService.upsert(embeddedChunks, metadata);

        verify(qdrantClient).upsertAsync(eq(collectionName), any(List.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void upsert_validChunks_createsCorrectNumberOfPoints() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<Points.UpdateResult> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(Points.UpdateResult.getDefaultInstance());
        when(qdrantClient.upsertAsync(eq(collectionName), any(List.class))).thenReturn(future);

        vectorStorageService.upsert(embeddedChunks, metadata);

        ArgumentCaptor<List<Points.PointStruct>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(qdrantClient).upsertAsync(eq(collectionName), pointsCaptor.capture());

        assertEquals(3, pointsCaptor.getValue().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void upsert_validChunks_includesCorrectMetadataInPayload() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<Points.UpdateResult> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(Points.UpdateResult.getDefaultInstance());
        when(qdrantClient.upsertAsync(eq(collectionName), any(List.class))).thenReturn(future);

        vectorStorageService.upsert(embeddedChunks, metadata);

        ArgumentCaptor<List<Points.PointStruct>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(qdrantClient).upsertAsync(eq(collectionName), pointsCaptor.capture());

        Points.PointStruct point = pointsCaptor.getValue().get(0);
        var payload = point.getPayloadMap();

        assertEquals(workspaceId.toString(), payload.get("workspaceId").getStringValue());
        assertEquals(projectId.toString(), payload.get("projectId").getStringValue());
        assertEquals(documentId.toString(), payload.get("documentId").getStringValue());
        assertEquals("Content 0", payload.get("content").getStringValue());
        assertEquals(0, payload.get("page").getIntegerValue());
        assertEquals(0, payload.get("startOffset").getIntegerValue());
        assertEquals(100, payload.get("endOffset").getIntegerValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    void upsert_validChunks_generatesCorrectPointId() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<Points.UpdateResult> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(Points.UpdateResult.getDefaultInstance());
        when(qdrantClient.upsertAsync(eq(collectionName), any(List.class))).thenReturn(future);

        vectorStorageService.upsert(embeddedChunks, metadata);

        ArgumentCaptor<List<Points.PointStruct>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(qdrantClient).upsertAsync(eq(collectionName), pointsCaptor.capture());

        Points.PointStruct point = pointsCaptor.getValue().get(0);
        String pointIdString = documentId + ":0:0";
        UUID expectedPointId = UUID.nameUUIDFromBytes(pointIdString.getBytes());

        assertEquals(expectedPointId.toString(), point.getId().getUuid());
    }

    @SuppressWarnings("unchecked")
    @Test
    void upsert_validChunks_setsVectorCorrectly() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<Points.UpdateResult> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(Points.UpdateResult.getDefaultInstance());
        when(qdrantClient.upsertAsync(eq(collectionName), any(List.class))).thenReturn(future);

        vectorStorageService.upsert(embeddedChunks, metadata);

        ArgumentCaptor<List<Points.PointStruct>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(qdrantClient).upsertAsync(eq(collectionName), pointsCaptor.capture());

        Points.PointStruct point = pointsCaptor.getValue().get(0);
        // The vector is stored in the Vectors wrapper
        assertTrue(point.hasVectors());
        assertNotNull(point.getVectors());
    }

    @SuppressWarnings("unchecked")
    @Test
    void upsert_qdrantError_throwsRuntimeException() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<Points.UpdateResult> future = mock(ListenableFuture.class);
        when(future.get()).thenThrow(new ExecutionException(new RuntimeException("Qdrant error")));
        when(qdrantClient.upsertAsync(eq(collectionName), any(List.class))).thenReturn(future);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> vectorStorageService.upsert(embeddedChunks, metadata));

        assertTrue(exception.getMessage().contains("Failed to upsert embeddings to Qdrant"));
    }

    @Test
    void search_qdrantError_throwsRuntimeException() {
        when(props.collection()).thenReturn(collectionName);
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class)))
                .thenThrow(new RuntimeException("Qdrant search error"));

        List<UUID> documentIds = List.of(documentId);
        List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> vectorStorageService.search(workspaceId, projectId, documentIds, queryVector, 10, 0.5f));

        assertTrue(exception.getMessage().contains("Qdrant search failed"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_validRequest_callsQdrantWithCorrectCollection() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<List<Points.ScoredPoint>> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(List.of());
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class))).thenReturn(future);

        List<UUID> documentIds = List.of(documentId);
        List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);

        vectorStorageService.search(workspaceId, projectId, documentIds, queryVector, 10, 0.5f);

        ArgumentCaptor<Points.SearchPoints> searchCaptor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(searchCaptor.capture());

        assertEquals(collectionName, searchCaptor.getValue().getCollectionName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_validRequest_setsCorrectLimit() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<List<Points.ScoredPoint>> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(List.of());
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class))).thenReturn(future);

        List<UUID> documentIds = List.of(documentId);
        List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);

        vectorStorageService.search(workspaceId, projectId, documentIds, queryVector, 15, 0.5f);

        ArgumentCaptor<Points.SearchPoints> searchCaptor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(searchCaptor.capture());

        assertEquals(15, searchCaptor.getValue().getLimit());
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_validRequest_setsScoreThreshold() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<List<Points.ScoredPoint>> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(List.of());
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class))).thenReturn(future);

        List<UUID> documentIds = List.of(documentId);
        List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);

        vectorStorageService.search(workspaceId, projectId, documentIds, queryVector, 10, 0.7f);

        ArgumentCaptor<Points.SearchPoints> searchCaptor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(searchCaptor.capture());

        assertEquals(0.7f, searchCaptor.getValue().getScoreThreshold(), 0.001f);
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_nullScoreThreshold_omitsThreshold() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<List<Points.ScoredPoint>> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(List.of());
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class))).thenReturn(future);

        List<UUID> documentIds = List.of(documentId);
        List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);

        vectorStorageService.search(workspaceId, projectId, documentIds, queryVector, 10, null);

        ArgumentCaptor<Points.SearchPoints> searchCaptor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(searchCaptor.capture());

        assertFalse(searchCaptor.getValue().hasScoreThreshold());
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_validRequest_passesQueryVector() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<List<Points.ScoredPoint>> future = mock(ListenableFuture.class);
        when(future.get()).thenReturn(List.of());
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class))).thenReturn(future);

        List<UUID> documentIds = List.of(documentId);
        List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);

        vectorStorageService.search(workspaceId, projectId, documentIds, queryVector, 10, 0.5f);

        ArgumentCaptor<Points.SearchPoints> searchCaptor = ArgumentCaptor.forClass(Points.SearchPoints.class);
        verify(qdrantClient).searchAsync(searchCaptor.capture());

        List<Float> capturedVector = searchCaptor.getValue().getVectorList();
        assertEquals(3, capturedVector.size());
        assertEquals(0.1f, capturedVector.get(0), 0.001f);
    }

    @SuppressWarnings("unchecked")
    @Test
    void search_returnsResultsFromQdrant() throws Exception {
        when(props.collection()).thenReturn(collectionName);
        ListenableFuture<List<Points.ScoredPoint>> future = mock(ListenableFuture.class);
        
        Points.ScoredPoint scoredPoint = Points.ScoredPoint.newBuilder()
                .setScore(0.95f)
                .build();
        
        when(future.get()).thenReturn(List.of(scoredPoint));
        when(qdrantClient.searchAsync(any(Points.SearchPoints.class))).thenReturn(future);

        List<UUID> documentIds = List.of(documentId);
        List<Float> queryVector = List.of(0.1f, 0.2f, 0.3f);

        List<Points.ScoredPoint> results = vectorStorageService.search(
                workspaceId, projectId, documentIds, queryVector, 10, 0.5f);

        assertEquals(1, results.size());
        assertEquals(0.95f, results.get(0).getScore(), 0.001f);
    }
}
