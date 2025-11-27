package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.EmbeddingRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingResponse;
import com.baskaaleksander.nuvine.infrastructure.ai.client.OpenAIEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final OpenAIEmbeddingClient embeddingClient;

    public EmbeddingResponse createEmbeddings(EmbeddingRequest request) {
        List<List<Float>> embeddings = createEmbedding(request.texts(), request.model());
        return new EmbeddingResponse(
                embeddings,
                request.model()
        );
    }

    private List<List<Float>> createEmbedding(List<String> input, String model) {
        List<List<Float>> embeddings;
        try {
            embeddings = embeddingClient.embed(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return embeddings;
    }

}
