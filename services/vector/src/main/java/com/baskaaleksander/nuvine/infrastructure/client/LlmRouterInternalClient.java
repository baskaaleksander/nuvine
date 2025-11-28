package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.application.dto.EmbeddingRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingResponse;
import com.baskaaleksander.nuvine.infrastructure.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "llm-router-internal",
        url = "${application.config.api-base-url}",
        contextId = "llmRouterInternalClient",
        configuration = InternalFeignConfig.class
)
public interface LlmRouterInternalClient {

    @PostMapping("/internal/llm/embeddings")
    EmbeddingResponse embed(@RequestBody EmbeddingRequest request);
}
