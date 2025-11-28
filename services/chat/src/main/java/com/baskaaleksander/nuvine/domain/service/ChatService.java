package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CompletionRequest;
import com.baskaaleksander.nuvine.infrastructure.client.LlmRouterServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final LlmRouterServiceClient llmRouterServiceClient;

    public String completion(CompletionRequest request) {
        return llmRouterServiceClient.completion(request);
    }
}
