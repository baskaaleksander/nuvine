package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.infrastructure.client.LlmRouterInternalClient;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorSearchService {

    private final VectorStorageService storageService;
    private final LlmRouterInternalClient llmRouterInternalClient;

    public VectorSearchResponse searchByText(TextVectorSearchRequest request) {
        log.info("VECTOR_SEARCH_BY_TEXT START projectId={}", request.projectId());
        EmbeddingResponse embeddingResponse = llmRouterInternalClient.embed(
                new EmbeddingRequest(List.of(request.query()), "text-embedding-3-small")
        );
        List<Float> queryVector = embeddingResponse.embeddings().get(0);

        return search(
                new VectorSearchRequest(
                        request.workspaceId(),
                        request.projectId(),
                        request.documentIds(),
                        queryVector,
                        request.topK(),
                        request.threshold()
                )
        );
    }

    public VectorSearchResponse search(VectorSearchRequest req) {
        List<Points.ScoredPoint> searchResults = storageService.search(
                req.workspaceId(),
                req.projectId(),
                req.documentIds(),
                req.query(),
                req.topK(),
                req.threshold()
        );

        List<VectorSearchResponse.VectorSearchMatch> matches = new ArrayList<>();

        for (var point : searchResults) {
            var fields = point.getPayloadMap();

            var documentIdValue = fields.get("documentId");
            var pageValue = fields.get("page");
            var startOffsetValue = fields.get("startOffset");
            var endOffsetValue = fields.get("endOffset");
            var contentValue = fields.get("content");

            matches.add(
                    new VectorSearchResponse.VectorSearchMatch(
                            UUID.fromString(documentIdValue.getStringValue()),
                            (int) pageValue.getIntegerValue(),
                            (int) startOffsetValue.getIntegerValue(),
                            (int) endOffsetValue.getIntegerValue(),
                            contentValue.getStringValue(),
                            point.getScore()
                    )
            );
        }
        return new VectorSearchResponse(matches);
    }
}
