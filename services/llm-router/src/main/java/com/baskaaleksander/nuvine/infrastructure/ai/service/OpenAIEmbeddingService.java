package com.baskaaleksander.nuvine.infrastructure.ai.service;

import com.baskaaleksander.nuvine.application.dto.EmbeddingApiRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingApiResponse;
import com.baskaaleksander.nuvine.infrastructure.ai.client.OpenAIEmbeddingClient;
import com.baskaaleksander.nuvine.infrastructure.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAIEmbeddingService {

    private final OpenAIEmbeddingClient client;
    private final OpenAIConfig.OpenAIProperties props;

    public List<List<Float>> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        EmbeddingApiRequest requestBody = new EmbeddingApiRequest(
                props.embeddingModel(),
                texts
        );

        EmbeddingApiResponse response = client.createEmbedding(requestBody);

        if (response == null || response.data() == null) {
            throw new IllegalStateException("Empty response from OpenAI embeddings API");
        }

        return response.data()
                .stream()
                .sorted(Comparator.comparingInt(EmbeddingApiResponse.EmbeddingData::index))
                .map(EmbeddingApiResponse.EmbeddingData::embedding)
                .toList();
    }
}
