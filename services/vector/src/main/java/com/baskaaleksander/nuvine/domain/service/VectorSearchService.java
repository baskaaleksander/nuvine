package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.TextVectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchResponse;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return null;
    }
}
