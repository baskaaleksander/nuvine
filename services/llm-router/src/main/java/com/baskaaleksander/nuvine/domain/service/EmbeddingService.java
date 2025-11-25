package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.EmbeddingRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingResponse;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.EmbeddingCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final OpenAIClient client;

    public EmbeddingResponse createEmbeddings(EmbeddingRequest request) {
        List<EmbeddingResponse.Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < request.texts().size(); i++) {
            embeddings.add(new EmbeddingResponse.Embedding(i, createEmbedding(request.texts().get(i), request.model())));
        }

        return new EmbeddingResponse(
                embeddings,
                request.model()
        );
    }

    private List<Float> createEmbedding(String input, String model) {
        EmbeddingCreateParams createParams = EmbeddingCreateParams.builder()
                .input(input)
                .model(model)
                .build();

        CreateEmbeddingResponse response;
        try {
            response = client.embeddings().create(createParams);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return response.data().getFirst().embedding();
    }

}
