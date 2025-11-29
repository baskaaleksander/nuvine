package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.ConversationMessageMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.client.LlmRouterServiceClient;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final LlmRouterServiceClient llmRouterServiceClient;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationMessageMapper mapper;
    private final WorkspaceServiceClient workspaceServiceClient;

    public ConversationMessageResponse completion(CompletionRequest request, String userId) {

        checkWorkspaceAccess(request.workspaceId());
        List<UUID> documentIds = getDocumentIdsInProject(request.projectId());

        if (request.documentIds() != null && !request.documentIds().isEmpty()) {
            Set<UUID> available = new HashSet<>(documentIds);

            if (!available.containsAll(request.documentIds())) {
                throw new RuntimeException("DOCUMENTS_NOT_FOUND");
            }
        }

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
        CompletionResponse completion;

        if (request.freeMode()) {
            completion = getCompletionResponse(request.message(), request.model(), messages, convoId);
        } else {
            // todo: add context building here
            completion = getCompletionResponse(request.message(), request.model(), messages, convoId);
        }

        conversationMessageRepository.save(
                ConversationMessage.builder()
                        .conversationId(convoId)
                        .content(request.message())
                        .role(ConversationRole.USER)
                        .modelUsed(request.model())
                        .tokensCost(completion.tokensIn())
                        .ownerId(ownerUUID)
                        .cost(0)
                        .build()
        );

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
                assistantMessage.getConversationId(),
                assistantMessage.getContent(),
                assistantMessage.getRole(),
                assistantMessage.getModelUsed(),
                assistantMessage.getTokensCost(),
                assistantMessage.getOwnerId(),
                assistantMessage.getCreatedAt()
        );
    }

    private void checkWorkspaceAccess(UUID workspaceId) {
        try {
            workspaceServiceClient.checkWorkspaceAccess(workspaceId);
        } catch (FeignException e) {
            int status = e.status();
            if (status == 404) {
                throw new RuntimeException("WORKSPACE_NOT_FOUND");
            } else if (status == 403) {
                throw new RuntimeException("WORKSPACE_ACCESS_DENIED");
            }
            log.error("WORKSPACE_ACCESS_CHECK_FAILED workspaceId={}", workspaceId, e);
            throw new RuntimeException("WORKSPACE_ACCESS_CHECK_FAILED", e);
        } catch (Exception e) {
            log.error("WORKSPACE_ACCESS_CHECK_FAILED workspaceId={}", workspaceId, e);
            throw new RuntimeException("WORKSPACE_ACCESS_CHECK_FAILED", e);
        }
    }

    private List<UUID> getDocumentIdsInProject(UUID projectId) {
        try {
            return workspaceServiceClient.getDocumentIdsInProject(projectId);
        } catch (FeignException e) {
            int status = e.status();
            if (status == 404) {
                throw new RuntimeException("PROJECT_NOT_FOUND");
            } else if (status == 403) {
                throw new RuntimeException("PROJECT_ACCESS_DENIED");
            }
            log.error("PROJECT_ACCESS_CHECK_FAILED projectId={}", projectId, e);
            throw new RuntimeException("PROJECT_ACCESS_CHECK_FAILED", e);
        } catch (Exception e) {
            log.error("PROJECT_ACCESS_CHECK_FAILED projectId={}", projectId, e);
            throw new RuntimeException("PROJECT_ACCESS_CHECK_FAILED", e);
        }
    }

    private CompletionResponse getCompletionResponse(String prompt, String model, List<CompletionLlmRouterRequest.Message> messages, UUID convoId) {
        try {
            return llmRouterServiceClient.completion(new CompletionLlmRouterRequest(prompt, model, messages));
        } catch (Exception e) {
            log.error("CHAT_COMPLETION FAILED convoId={}", convoId, e);
            throw new RuntimeException("CHAT_COMPLETION FAILED", e);
        }
    }

    private List<CompletionLlmRouterRequest.Message> buildChatMemory(UUID convoId, int memorySize) {
        return conversationMessageRepository.findByConversationId(convoId, memorySize * 2).stream().map(
                message -> new CompletionLlmRouterRequest.Message(message.getRole().name().toLowerCase(), message.getContent())
        ).toList();
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
