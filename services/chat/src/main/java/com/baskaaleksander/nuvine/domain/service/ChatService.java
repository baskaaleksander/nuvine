package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CompletionLlmRouterRequest;
import com.baskaaleksander.nuvine.application.dto.CompletionRequest;
import com.baskaaleksander.nuvine.application.dto.CompletionResponse;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.client.LlmRouterServiceClient;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final LlmRouterServiceClient llmRouterServiceClient;
    private final ConversationMessageRepository conversationMessageRepository;

    public CompletionResponse completion(CompletionRequest request) {
        UUID convoId;
        List<CompletionLlmRouterRequest.Message> messages = null;
        if (request.conversationId() == null) {
            convoId = UUID.randomUUID();
        } else {
            convoId = request.conversationId();
            messages = conversationMessageRepository.findByConversationId(convoId, request.memorySize() * 2).stream().map(
                    message -> new CompletionLlmRouterRequest.Message(message.getRole().name().toLowerCase(), message.getContent())
            ).toList();
        }
        var completion = llmRouterServiceClient.completion(new CompletionLlmRouterRequest(request.message(), request.model(), messages));

        conversationMessageRepository.save(
                ConversationMessage.builder()
                        .conversationId(convoId)
                        .content(request.message())
                        .role(ConversationRole.USER)
                        .modelUsed(request.model())
                        .tokensCost(completion.tokensIn())
                        .cost(0)
                        .build()
        );

        conversationMessageRepository.save(
                ConversationMessage.builder()
                        .conversationId(convoId)
                        .content(completion.content())
                        .role(ConversationRole.ASSISTANT)
                        .modelUsed(request.model())
                        .tokensCost(completion.tokensOut())
                        .cost(0)
                        .build()
        );

        return completion;
    }
}
