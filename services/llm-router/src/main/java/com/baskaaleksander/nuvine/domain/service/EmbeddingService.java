package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.EmbeddingRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingResponse;
import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
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

    public List<EmbeddedChunk> createEmbeddings(List<Chunk> chunks) {
        List<List<Float>> embeddings = createEmbedding(chunks.stream().map(Chunk::content).toList());
        return chunks.stream()
                .map(chunk -> new EmbeddedChunk(chunk.documentId(), chunk.page(), chunk.startOffset(), chunk.endOffset(), embeddings.get(chunk.index()), chunk.content(), chunk.index()))
                .toList();
    }

    public EmbeddingResponse createEmbeddings(EmbeddingRequest request) {
        List<List<Float>> embeddings = createEmbedding(request.texts());
        return new EmbeddingResponse(
                embeddings,
                request.model()
        );
    }

    private List<List<Float>> createEmbedding(List<String> input) {
        List<List<Float>> embeddings;
        try {
            embeddings = embeddingClient.embed(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return embeddings;
    }

}
