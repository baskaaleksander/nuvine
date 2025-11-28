package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.TextVectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchResponse;
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

    public VectorSearchResponse searchByText(TextVectorSearchRequest request) {
        return null;
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

            matches.add(
                    new VectorSearchResponse.VectorSearchMatch(
                            UUID.fromString(fields.get("documentId").getStringValue()),
                            (int) fields.get("page").getIntegerValue(),
                            (int) fields.get("startOffset").getIntegerValue(),
                            (int) fields.get("endOffset").getIntegerValue(),
                            fields.get("content").getStringValue(),
                            point.getScore()
                    )
            );
        }
        return new VectorSearchResponse(matches);
    }
}
