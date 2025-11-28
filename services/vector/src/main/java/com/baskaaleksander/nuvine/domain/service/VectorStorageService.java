package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.ChunkMetadata;
import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.infrastructure.config.QdrantConfig;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStorageService {

    private final QdrantClient qdrantClient;
    private final QdrantConfig.QdrantProperties props;


    public void upsert(List<EmbeddedChunk> chunks, ChunkMetadata metadata) {

        List<Points.PointStruct> points = chunks.stream()
                .map(c -> toPoint(c, metadata))
                .toList();

        try {
            qdrantClient.upsertAsync(props.collection(), points).get();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upsert embeddings to Qdrant", ex);
        }
    }

    public List<Points.ScoredPoint> search(
            UUID workspaceId,
            UUID projectId,
            List<UUID> documentIds,
            List<Float> queryVector,
            int topK,
            Float scoreThreshold
    ) {
        try {
            return searchWithFilter(workspaceId, projectId, documentIds, queryVector, topK, scoreThreshold);
        } catch (Exception e) {
            throw new RuntimeException("Qdrant search failed", e);
        }
    }

    private List<Points.ScoredPoint> searchWithFilter(
            UUID workspaceId,
            UUID projectId,
            List<UUID> documentIds,
            List<Float> queryVector,
            int topK,
            Float scoreThreshold
    ) throws Exception {

        Common.Filter.Builder filterBuilder = Common.Filter.newBuilder()
                .addMust(matchKeyword("workspaceId", workspaceId.toString()))
                .addMust(matchKeyword("projectId", projectId.toString()));

        if (documentIds != null && !documentIds.isEmpty()) {
            for (UUID docId : documentIds) {
                filterBuilder.addShould(matchKeyword("documentId", docId.toString()));
            }
        }

        Common.Filter filter = filterBuilder.build();

        Points.SearchPoints.Builder searchBuilder = Points.SearchPoints.newBuilder()
                .setCollectionName(props.collection())
                .addAllVector(queryVector)
                .setLimit(topK)
                .setFilter(filter);

        if (scoreThreshold != null) {
            searchBuilder.setScoreThreshold(scoreThreshold);
        }

        Points.SearchPoints searchRequest = searchBuilder.build();

        List<Points.ScoredPoint> results = qdrantClient.searchAsync(searchRequest).get();

        return results;
    }

    private Points.PointStruct toPoint(EmbeddedChunk c, ChunkMetadata metadata) {
        return Points.PointStruct.newBuilder()
                .setId(id(buildPointId(c)))
                .setVectors(vectors(c.embedding()))
                .putAllPayload(Map.of(
                        "workspaceId", value(metadata.workspaceId().toString()),
                        "projectId", value(metadata.projectId().toString()),
                        "documentId", value(c.documentId().toString()),
                        "page", value(c.page()),
                        "startOffset", value(c.startOffset()),
                        "endOffset", value(c.endOffset())
                ))
                .build();
    }

    private UUID buildPointId(EmbeddedChunk c) {
        return UUID.nameUUIDFromBytes(
                (c.documentId() + ":" + c.page() + ":" + c.startOffset()).getBytes()
        );
    }
}
