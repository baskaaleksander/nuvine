package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.infrastructure.config.QdrantConfig;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStorageService {

    private final QdrantClient qdrantClient;
    private final QdrantConfig.QdrantProperties props;


    public void upsert(List<EmbeddedChunk> chunks) {

        List<Points.PointStruct> points = chunks.stream()
                .map(this::toPoint)
                .toList();

        try {
            qdrantClient.upsertAsync(props.collection(), points).get();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upsert embeddings to Qdrant", ex);
        }
    }

    private Points.PointStruct toPoint(EmbeddedChunk c) {
        return Points.PointStruct.newBuilder()
                .setId(id(buildPointId(c)))
                .setVectors(vectors(c.embedding()))
                .putAllPayload(Map.of(
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
