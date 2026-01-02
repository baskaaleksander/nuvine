package com.baskaaleksander.nuvine.infrastructure.ai.client;

import com.baskaaleksander.nuvine.application.dto.EmbeddingApiRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingApiResponse;
import com.baskaaleksander.nuvine.infrastructure.config.OpenAIFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "openai-client",
        url = "${openai.base-url}",
        configuration = OpenAIFeignConfig.class
)
public interface OpenAIEmbeddingClient {

    @PostMapping("/embeddings")
    EmbeddingApiResponse createEmbedding(@RequestBody EmbeddingApiRequest request);
}
