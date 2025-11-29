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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final LlmRouterServiceClient llmRouterServiceClient;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationMessageMapper mapper;
    private final WorkspaceServiceClient workspaceServiceClient;
    private final WebClient llmRouterWebClient;

    public ConversationMessageResponse completion(CompletionRequest request, String userId) {

        checkWorkspaceAccess(request.workspaceId());
//        List<UUID> documentIds = getDocumentIdsInProject(request.projectId());
//
//        if (request.documentIds() != null && !request.documentIds().isEmpty()) {
//            Set<UUID> available = new HashSet<>(documentIds);
//
//            if (!available.containsAll(request.documentIds())) {
//                throw new RuntimeException("DOCUMENTS_NOT_FOUND");
//            }
//        }

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

    public SseEmitter completionStream(CompletionRequest request, String userId) {
        ChatContext ctx = prepareChatContext(request, userId);

        SseEmitter emitter = new SseEmitter(0L);
        StringBuilder answerBuilder = new StringBuilder();

        AtomicInteger tokensIn = new AtomicInteger(0);
        AtomicInteger tokensOut = new AtomicInteger(0);

        CompletionLlmRouterRequest routerRequest =
                new CompletionLlmRouterRequest(request.message(), request.model(), ctx.messages());

        llmRouterWebClient.post()
                .uri("/api/v1/internal/llm/completion/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)
                .bodyValue(routerRequest)
                .retrieve()
                .bodyToFlux(LlmChunk.class)
                .doOnNext(chunk -> {
                    try {
                        switch (chunk.type()) {
                            case "delta" -> {
                                answerBuilder.append(chunk.content());
                                emitter.send(SseEmitter.event()
                                        .name("delta")
                                        .data(chunk.content()));
                            }
                            case "usage" -> {
                                tokensIn.set(chunk.tokensIn());
                                tokensOut.set(chunk.tokensOut());

                                emitter.send(SseEmitter.event()
                                        .name("usage")
                                        .data(new TokenUsage(chunk.tokensIn(), chunk.tokensOut())));
                            }
                            case "info" -> {
                                emitter.send(SseEmitter.event()
                                        .name("info")
                                        .data(chunk.content()));
                            }
                            case "done" -> {
                                emitter.send(SseEmitter.event()
                                        .name("metadata")
                                        .data(new StreamEventMetadata(ctx.conversationId)));
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data("done"));
                            }
                            default -> {
                                log.warn("Unknown chunk type: {}", chunk.type());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to send SSE chunk convoId={}", ctx.conversationId(), e);
                    }
                })
                .doOnError(ex -> {
                    log.error("CHAT_COMPLETION_STREAM FAILED convoId={}", ctx.conversationId(), ex);
                    try {
                        emitter.completeWithError(ex);
                    } catch (Exception ignored) {
                    }
                })
                .doOnComplete(() -> {
                    try {
                        persistConversationMessages(
                                ctx,
                                request,
                                answerBuilder.toString(),
                                tokensIn.get(),
                                tokensOut.get()
                        );
                    } catch (Exception e) {
                        log.error("Failed to persist messages after stream convoId={}", ctx.conversationId(), e);
                    } finally {
                        emitter.complete();
                    }
                })
                .subscribe();

        return emitter;
    }

    private void persistConversationMessages(
            ChatContext ctx,
            CompletionRequest request,
            String assistantContent,
            int tokensIn,
            int tokensOut
    ) {
        log.info("CHAT_COMPLETION_STREAM END convoId={}", ctx.conversationId());

        ConversationMessage userMessage = ConversationMessage.builder()
                .conversationId(ctx.conversationId())
                .content(request.message())
                .role(ConversationRole.USER)
                .modelUsed(request.model())
                .ownerId(ctx.ownerId())
                .tokensCost(tokensIn)
                .cost(0)
                .build();

        conversationMessageRepository.save(userMessage);

        ConversationMessage assistantMessage = conversationMessageRepository.save(
                ConversationMessage.builder()
                        .conversationId(ctx.conversationId())
                        .content(assistantContent)
                        .role(ConversationRole.ASSISTANT)
                        .modelUsed(request.model())
                        .tokensCost(tokensOut)
                        .ownerId(ctx.ownerId())
                        .cost(0)
                        .build()
        );
    }

    private ChatContext prepareChatContext(CompletionRequest request, String userId) {
        checkWorkspaceAccess(request.workspaceId());
        List<UUID> documentIds = getDocumentIdsInProject(request.projectId());

        if (request.documentIds() != null && !request.documentIds().isEmpty()) {
            Set<UUID> available = new HashSet<>(documentIds);

            List<UUID> missing = request.documentIds().stream()
                    .filter(id -> !available.contains(id))
                    .toList();

            if (!missing.isEmpty()) {
                log.warn("DOCUMENTS_NOT_FOUND projectId={} missing={}", request.projectId(), missing);
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

        return new ChatContext(convoId, messages, ownerUUID);
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
        return conversationMessageRepository.findUserConversations(UUID.fromString(ownerId))
                .stream()
                .map(cm -> new UserConversationResponse(
                        cm.conversationId(),
                        cleanMarkdown(cm.lastMessage()).substring(0,
                                Math.min(cleanMarkdown(cm.lastMessage()).length(), 100)),
                        cm.lastMessageAt()
                ))
                .toList();
    }


    private String cleanMarkdown(String input) {
        if (input == null) return "";

        String cleaned = input;

        cleaned = cleaned.replaceAll("(?m)^#{1,6}\\s*", "");
        cleaned = cleaned.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        cleaned = cleaned.replaceAll("\\*(.*?)\\*", "$1");
        cleaned = cleaned.replaceAll("~~(.*?)~~", "$1");
        cleaned = cleaned.replaceAll("`([^`]*)`", "$1");
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "");
        cleaned = cleaned.replaceAll(">\\s*", "");
        cleaned = cleaned.replaceAll("[-*+]\\s+", "");
        cleaned = cleaned.replaceAll("\\r?\\n", " ");

        return cleaned.trim();
    }

    private record ChatContext(
            UUID conversationId,
            List<CompletionLlmRouterRequest.Message> messages,
            UUID ownerId
    ) {
    }
}
