package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.application.dto.CompletionLlmRouterRequest;
import com.baskaaleksander.nuvine.application.dto.CompletionResponse;
import com.baskaaleksander.nuvine.infrastructure.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "llm-router-service",
        url = "${application.config.api-base-url}",
        contextId = "llmRouterServiceClient",
        configuration = InternalFeignConfig.class
)
public interface LlmRouterServiceClient {
    @PostMapping("/internal/llm/completions")
    CompletionResponse completion(
            @RequestBody CompletionLlmRouterRequest request
    );
}
