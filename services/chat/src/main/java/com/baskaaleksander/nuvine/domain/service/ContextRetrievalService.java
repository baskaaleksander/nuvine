package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.TextVectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchResponse;
import com.baskaaleksander.nuvine.infrastructure.client.VectorServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContextRetrievalService {

    private final VectorServiceClient vectorServiceClient;

    public List<String> retrieveContext(
            UUID workspaceId,
            UUID projectId,
            List<UUID> documentIds,
            String query,
            int topK,
            float threshold
    ) {

        List<String> context;
        try {
            context = vectorServiceClient.searchText(
                    new TextVectorSearchRequest(
                            workspaceId,
                            projectId,
                            documentIds,
                            query,
                            topK,
                            threshold
                    )
            ).matches().stream().map(VectorSearchResponse.VectorSearchMatch::content).toList();
        } catch (Exception e) {
            log.error("CONTEXT_RETRIEVAL FAILED workspaceId={}, projectId={}, documentIds={}, topK={}, threshold={} ", workspaceId, projectId, documentIds, topK, threshold, e);
            throw e;
        }
        return context;
    }
}
