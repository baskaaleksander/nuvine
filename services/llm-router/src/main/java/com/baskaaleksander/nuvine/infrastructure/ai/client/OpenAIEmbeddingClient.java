package com.baskaaleksander.nuvine.infrastructure.ai.client;

import com.baskaaleksander.nuvine.application.dto.EmbeddingApiRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingApiResponse;
import com.baskaaleksander.nuvine.infrastructure.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenAIEmbeddingClient {

    private final RestTemplate openAiRestTemplate;
    private final OpenAIConfig.OpenAIProperties props;

    public List<List<Float>> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        EmbeddingApiRequest requestBody = new EmbeddingApiRequest(
                props.embeddingModel(),
                texts
        );

        String url = props.baseUrl() + "/embeddings";

        EmbeddingApiResponse response = openAiRestTemplate.postForObject(
                url,
                requestBody,
                EmbeddingApiResponse.class
        );

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
