package com.baskaaleksander.nuvine.integration.service;

import com.baskaaleksander.nuvine.domain.model.ChunkMetadata;
import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.domain.service.VectorStorageService;
import com.baskaaleksander.nuvine.infrastructure.config.QdrantConfig;
import com.baskaaleksander.nuvine.integration.base.BaseIntegrationTest;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantIntegrationIT extends BaseIntegrationTest {

    @Autowired
    private VectorStorageService vectorStorageService;

    @Autowired
    private QdrantClient qdrantClient;

    @Autowired
    private QdrantConfig.QdrantProperties qdrantProperties;

    private UUID workspaceId;
    private UUID projectId;
    private UUID documentId;

    @BeforeEach
    void setUp() throws Exception {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        ensureCollectionExists();
    }

    private void ensureCollectionExists() throws Exception {
        String collectionName = qdrantProperties.collection();
        try {
            qdrantClient.getCollectionInfoAsync(collectionName).get();
        } catch (ExecutionException e) {
            qdrantClient.createCollectionAsync(
                    collectionName,
                    Collections.VectorParams.newBuilder()
                            .setSize(1536)
                            .setDistance(Collections.Distance.Cosine)
                            .build()
            ).get();
        }
    }

    @Test
    void shouldUpsertPointsToQdrant() throws Exception {
        List<EmbeddedChunk> chunks = createTestChunks(3);
        ChunkMetadata metadata = new ChunkMetadata(workspaceId, projectId);

        vectorStorageService.upsert(chunks, metadata);

        Thread.sleep(500);

        List<Points.ScoredPoint> results = vectorStorageService.search(
                workspaceId, projectId, List.of(documentId),
                chunks.get(0).embedding(), 10, 0.0f
        );

        assertThat(results).isNotEmpty();
    }

    @Test
    void shouldSearchByVectorWithFilters() throws Exception {
        List<EmbeddedChunk> chunks = createTestChunks(5);
        ChunkMetadata metadata = new ChunkMetadata(workspaceId, projectId);

        vectorStorageService.upsert(chunks, metadata);
        Thread.sleep(500);

        List<Float> queryVector = chunks.get(0).embedding();
        List<Points.ScoredPoint> results = vectorStorageService.search(
                workspaceId, projectId, List.of(documentId),
                queryVector, 3, null
        );

        assertThat(results).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void shouldSearchWithScoreThreshold() throws Exception {
        List<EmbeddedChunk> chunks = createTestChunks(3);
        ChunkMetadata metadata = new ChunkMetadata(workspaceId, projectId);

        vectorStorageService.upsert(chunks, metadata);
        Thread.sleep(500);

        List<Float> queryVector = chunks.get(0).embedding();

        List<Points.ScoredPoint> resultsWithHighThreshold = vectorStorageService.search(
                workspaceId, projectId, List.of(documentId),
                queryVector, 10, 0.99f
        );

        List<Points.ScoredPoint> resultsWithLowThreshold = vectorStorageService.search(
                workspaceId, projectId, List.of(documentId),
                queryVector, 10, 0.1f
        );

        assertThat(resultsWithLowThreshold.size()).isGreaterThanOrEqualTo(resultsWithHighThreshold.size());
    }

    @Test
    void shouldReturnEmptyResultsForNonExistentWorkspace() {
        UUID nonExistentWorkspaceId = UUID.randomUUID();
        List<Float> queryVector = generateMockEmbedding(1536);

        List<Points.ScoredPoint> results = vectorStorageService.search(
                nonExistentWorkspaceId, projectId, List.of(documentId),
                queryVector, 10, null
        );

        assertThat(results).isEmpty();
    }

    @Test
    void shouldFilterByDocumentIds() throws Exception {
        UUID documentId1 = UUID.randomUUID();
        UUID documentId2 = UUID.randomUUID();

        List<EmbeddedChunk> chunks1 = createTestChunksForDocument(documentId1, 3);
        List<EmbeddedChunk> chunks2 = createTestChunksForDocument(documentId2, 3);

        ChunkMetadata metadata = new ChunkMetadata(workspaceId, projectId);

        vectorStorageService.upsert(chunks1, metadata);
        vectorStorageService.upsert(chunks2, metadata);
        Thread.sleep(500);

        List<Float> queryVector = generateMockEmbedding(1536);

        List<Points.ScoredPoint> resultsForDoc1 = vectorStorageService.search(
                workspaceId, projectId, List.of(documentId1),
                queryVector, 10, null
        );

        for (Points.ScoredPoint point : resultsForDoc1) {
            String docId = point.getPayloadMap().get("documentId").getStringValue();
            assertThat(docId).isEqualTo(documentId1.toString());
        }
    }

    private List<EmbeddedChunk> createTestChunks(int count) {
        return createTestChunksForDocument(documentId, count);
    }

    private List<EmbeddedChunk> createTestChunksForDocument(UUID docId, int count) {
        List<EmbeddedChunk> chunks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            chunks.add(new EmbeddedChunk(
                    docId,
                    i / 2,
                    i * 100,
                    (i + 1) * 100,
                    generateMockEmbedding(1536),
                    "Test content chunk " + i,
                    i
            ));
        }
        return chunks;
    }

    private List<Float> generateMockEmbedding(int dimensions) {
        List<Float> embedding = new ArrayList<>();
        for (int i = 0; i < dimensions; i++) {
            embedding.add((float) Math.random());
        }
        float norm = 0;
        for (Float f : embedding) {
            norm += f * f;
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < embedding.size(); i++) {
            embedding.set(i, embedding.get(i) / norm);
        }
        return embedding;
    }
}
