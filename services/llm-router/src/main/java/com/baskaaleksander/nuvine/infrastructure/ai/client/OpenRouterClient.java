package com.baskaaleksander.nuvine.infrastructure.ai.client;

import com.baskaaleksander.nuvine.application.dto.OpenRouterChatRequest;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatResponse;
import com.baskaaleksander.nuvine.infrastructure.config.OpenRouterFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "openrouter-client",
        url = "${openrouter.base-url}",
        configuration = OpenRouterFeignConfig.class
)
public interface OpenRouterClient {

    @PostMapping("/chat/completions")
    OpenRouterChatResponse createChatCompletion(OpenRouterChatRequest request);
}