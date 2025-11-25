package com.baskaaleksander.nuvine.domain.service;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final OpenAIClient client;

    public List<Float> createEmbedding(String input) {
        EmbeddingCreateParams createParams = EmbeddingCreateParams.builder()
                .input(input)
                .model(EmbeddingModel.TEXT_EMBEDDING_3_SMALL)
                .build();

        var response = client.embeddings().create(createParams);

        return response.data().getFirst().embedding();
    }
    
}
