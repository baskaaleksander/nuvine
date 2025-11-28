package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.ConversationMessageMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.client.LlmRouterServiceClient;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final LlmRouterServiceClient llmRouterServiceClient;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationMessageMapper mapper;

    public ConversationMessageResponse completion(CompletionRequest request, String userId) {
        UUID convoId;
        List<CompletionLlmRouterRequest.Message> messages = null;
        if (request.conversationId() == null) {
            convoId = UUID.randomUUID();
        } else {
            convoId = request.conversationId();
            messages = buildChatMemory(convoId, request.memorySize());
        }

        UUID ownerUUID = UUID.fromString(userId);

        log.info("CHAT_COMPLETION START convoId={}", convoId);

        ConversationMessage userMessage = ConversationMessage.builder()
                .conversationId(convoId)
                .content(request.message())
                .role(ConversationRole.USER)
                .modelUsed(request.model())
                .ownerId(ownerUUID)
                .cost(0)
                .build();

        conversationMessageRepository.save(userMessage);

        CompletionResponse completion;

        try {
            completion = llmRouterServiceClient.completion(new CompletionLlmRouterRequest(request.message(), request.model(), messages));
        } catch (Exception e) {
            log.error("CHAT_COMPLETION FAILED convoId={}", convoId, e);
            throw new RuntimeException("CHAT_COMPLETION FAILED", e);
        }

        userMessage.setTokensCost(completion.tokensIn());

        conversationMessageRepository.save(userMessage);

        ConversationMessage assistantMessage = conversationMessageRepository.save(
                ConversationMessage.builder()
                        .conversationId(convoId)
                        .content(completion.content())
                        .role(ConversationRole.ASSISTANT)
                        .modelUsed(request.model())
                        .tokensCost(completion.tokensOut())
                        .ownerId(ownerUUID)
                        .cost(0)
                        .build()
        );

        log.info("CHAT_COMPLETION END convoId={}", convoId);
        return new ConversationMessageResponse(
                assistantMessage.getId(),
                assistantMessage.getContent(),
                assistantMessage.getRole(),
                assistantMessage.getModelUsed(),
                assistantMessage.getTokensCost(),
                assistantMessage.getOwnerId(),
                assistantMessage.getCreatedAt()
        );
    }

    private List<CompletionLlmRouterRequest.Message> buildChatMemory(UUID convoId, int memorySize) {
        return conversationMessageRepository.findByConversationId(convoId, memorySize * 2).stream().map(
                message -> new CompletionLlmRouterRequest.Message(message.getRole().name().toLowerCase(), message.getContent())
        ).toList();
    }

    private List<String> buildRagContext() {
        return null;
    }

    public PagedResponse<ConversationMessageResponse> getMessages(UUID conversationId, String subject, PaginationRequest request) {
        Pageable pageable = PaginationUtil.getPageable(request);

        Page<ConversationMessage> page = conversationMessageRepository.findAllByConversationId(conversationId, pageable);

        List<ConversationMessageResponse> content = page.getContent().stream().map(mapper::toResponse).toList();

        return new PagedResponse<>(
                content,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.isLast(),
                page.hasNext()
        );
    }

    public List<UserConversationResponse> getUserConversations(String ownerId) {
        return conversationMessageRepository.findUserConversations(UUID.fromString(ownerId));
    }
}
