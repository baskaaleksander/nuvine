package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.infrastructure.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "llm-router=internal",
        url = "${application.config.api-base-url}",
        contextId = "llmRouterInternalClient",
        configuration = InternalFeignConfig.class
)
public interface LlmRouterInternalClient {
}
