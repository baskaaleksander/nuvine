package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.ConversationMessageMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.exception.CheckLimitNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.ContextNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.RequestLimitExceededException;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.infrastructure.client.LlmRouterServiceClient;
import com.baskaaleksander.nuvine.infrastructure.client.SubscriptionServiceClient;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final LlmRouterServiceClient llmRouterServiceClient;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationMessageMapper mapper;
    private final WebClient llmRouterWebClient;
    private final SubscriptionServiceClient subscriptionServiceClient;

    private final WorkspaceAccessService workspaceAccessService;
    private final RagPromptBuilder ragPromptBuilder;
    private final ConversationPersistenceService conversationPersistenceService;
    private final TokenCountingService tokenCountingService;
    private final ConversationCacheService conversationCacheService;

    public ConversationMessageResponse completion(CompletionRequest request, String userId) {
        UUID workspaceId = request.workspaceId();
        UUID projectId = request.projectId();

        log.info(
                "CHAT_COMPLETION START workspaceId={} projectId={} freeMode={} strictMode={}",
                workspaceId,
                projectId,
                request.freeMode(),
                request.strictMode()
        );

        ChatContext ctx;
        try {
            ctx = prepareChatContext(request, userId);
        } catch (ContextNotFoundException ex) {
            log.info(
                    "CHAT_COMPLETION CONTEXT_NOT_FOUND_STRICT workspaceId={} projectId={}",
                    workspaceId,
                    projectId
            );
            return handleContextNotFoundStrictModeSync(request, userId);
        }

        CheckLimitResult checkLimitResult = checkLimit(request, ctx.prompt());

        if (!checkLimitResult.approved()) {
            log.info(
                    "CHAT_COMPLETION LIMIT_EXCEEDED convoId={} workspaceId={} projectId={}",
                    ctx.conversationId(),
                    request.workspaceId(),
                    request.projectId()
            );
            throw new RequestLimitExceededException("Limit exceeded exception");
        }

//        CompletionLlmRouterRequest routerRequest =
//                new CompletionLlmRouterRequest(ctx.prompt(), request.model(), ctx.messages());

        log.info(
                "CHAT_COMPLETION LLM_CALL_START convoId={} model={} hasMemory={}",
                ctx.conversationId(),
                request.model(),
                ctx.messages() != null && !ctx.messages().isEmpty()
        );

        CompletionResponse completion = getCompletionResponse(
                ctx.prompt(),
                request.model(),
                ctx.messages(),
                ctx.conversationId()
        );

        ConversationMessage assistantMessage =
                conversationPersistenceService.persistSyncCompletion(
                        ctx.conversationId(),
                        request,
                        completion,
                        ctx.ownerId(),
                        checkLimitResult
                );

        log.info(
                "CHAT_COMPLETION END convoId={} workspaceId={} projectId={} model={} tokensIn={} tokensOut={}",
                ctx.conversationId(),
                workspaceId,
                projectId,
                request.model(),
                completion.tokensIn(),
                completion.tokensOut()
        );

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

    private ConversationMessageResponse handleContextNotFoundStrictModeSync(
            CompletionRequest request,
            String userId
    ) {
        UUID conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID();

        UUID ownerId = UUID.fromString(userId);

        String assistantContent =
                "I couldn't find any relevant context in your documents for this question. " +
                        "Please adjust your filters, select different documents or upload more data.";

        log.info(
                "CHAT_COMPLETION STRICT_NO_CONTEXT_RESPONSE convoId={} workspaceId={} projectId={}",
                conversationId,
                request.workspaceId(),
                request.projectId()
        );

        ConversationMessage assistantMessage =
                conversationPersistenceService.persistStrictModeNoContext(
                        conversationId,
                        request,
                        assistantContent,
                        ownerId
                );

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
        SseEmitter emitter = new SseEmitter(0L);
        StringBuilder answerBuilder = new StringBuilder();

        AtomicInteger tokensIn = new AtomicInteger(0);
        AtomicInteger tokensOut = new AtomicInteger(0);

        log.info(
                "CHAT_COMPLETION_STREAM START workspaceId={} projectId={} freeMode={} strictMode={}",
                request.workspaceId(),
                request.projectId(),
                request.freeMode(),
                request.strictMode()
        );

        ChatContext ctx;
        try {
            ctx = prepareChatContext(request, userId);
        } catch (ContextNotFoundException ex) {
            log.info(
                    "CHAT_COMPLETION_STREAM CONTEXT_NOT_FOUND_STRICT workspaceId={} projectId={}",
                    request.workspaceId(),
                    request.projectId()
            );
            handleContextNotFoundStrictMode(emitter, request, userId);
            return emitter;
        }
        CheckLimitResult checkLimitResult = checkLimit(request, ctx.prompt());

        if (!checkLimitResult.approved()) {
            log.info(
                    "CHAT_COMPLETION_STREAM LIMIT_EXCEEDED convoId={} workspaceId={} projectId={}",
                    ctx.conversationId(),
                    request.workspaceId(),
                    request.projectId()
            );
            throw new RequestLimitExceededException("Limit exceeded exception");
        }

        log.info(
                "CHAT_COMPLETION_STREAM CONTEXT_PREPARED convoId={} workspaceId={} projectId={}",
                ctx.conversationId(),
                request.workspaceId(),
                request.projectId()
        );


        CompletionLlmRouterRequest routerRequest =
                new CompletionLlmRouterRequest(ctx.prompt(), request.model(), ctx.messages());

        llmRouterWebClient.post()
                .uri("/api/v1/internal/llm/completion/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)
                .bodyValue(routerRequest)
                .retrieve()
                .bodyToFlux(LlmChunk.class)
                .doOnSubscribe(sub -> log.info(
                        "CHAT_COMPLETION_STREAM LLM_CALL_START convoId={} model={}",
                        ctx.conversationId(),
                        request.model()
                ))
                .doOnNext(chunk -> handleChunk(chunk, emitter, answerBuilder, tokensIn, tokensOut, ctx))
                .doOnError(ex -> handleStreamError(ex, emitter, ctx))
                .doOnComplete(() -> handleStreamComplete(
                        emitter,
                        ctx,
                        request,
                        answerBuilder.toString(),
                        tokensIn.get(),
                        tokensOut.get(),
                        checkLimitResult
                ))
                .subscribe();

        return emitter;
    }

    private CheckLimitResult checkLimit(CompletionRequest request, String prompt) {
        long inputTokens = tokenCountingService.count(prompt);

        String provider = request.model().split("/")[0];
        String model = request.model().split("/")[1];
        log.info(
                "CHAT_COMPLETION_STREAM CHECK_LIMIT convoId={} workspaceId={} projectId={} model={} provider={} inputTokens={}",
                request.conversationId(),
                request.workspaceId(),
                request.projectId(),
                model,
                provider,
                inputTokens
        );

        CheckLimitRequest checkLimitRequest =
                new CheckLimitRequest(
                        request.workspaceId(),
                        model,
                        provider,
                        inputTokens
                );
        CheckLimitResult checkLimitResult;
        try {
            checkLimitResult = subscriptionServiceClient.checkLimit(checkLimitRequest);
        } catch (FeignException e) {
            log.error("Check limit failed", e);
            int status = e.status();
            if (status == 404) {
                throw new CheckLimitNotFoundException("Check limit not found exception");
            }
            throw new RuntimeException(e);
        }

        return checkLimitResult;
    }

    private void handleChunk(
            LlmChunk chunk,
            SseEmitter emitter,
            StringBuilder answerBuilder,
            AtomicInteger tokensIn,
            AtomicInteger tokensOut,
            ChatContext ctx
    ) {
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

                    log.info(
                            "CHAT_COMPLETION_STREAM USAGE convoId={} tokensIn={} tokensOut={}",
                            ctx.conversationId(),
                            chunk.tokensIn(),
                            chunk.tokensOut()
                    );

                    emitter.send(SseEmitter.event()
                            .name("usage")
                            .data(new TokenUsage(chunk.tokensIn(), chunk.tokensOut())));
                }
                case "info" -> emitter.send(SseEmitter.event()
                        .name("info")
                        .data(chunk.content()));
                case "done" -> {
                    log.info("CHAT_COMPLETION_STREAM LLM_CALL_DONE convoId={}", ctx.conversationId());
                    emitter.send(SseEmitter.event()
                            .name("metadata")
                            .data(new StreamEventMetadata(ctx.conversationId())));
                    emitter.send(SseEmitter.event()
                            .name("done")
                            .data("done"));
                }
                default -> log.warn("CHAT_COMPLETION_STREAM UNKNOWN_CHUNK_TYPE convoId={} type={}",
                        ctx.conversationId(),
                        chunk.type()
                );
            }
        } catch (Exception e) {
            log.error("CHAT_COMPLETION_STREAM SSE_SEND_FAILED convoId={}", ctx.conversationId(), e);
        }
    }

    private void handleStreamError(Throwable ex, SseEmitter emitter, ChatContext ctx) {
        log.error("CHAT_COMPLETION_STREAM FAILED convoId={}", ctx.conversationId(), ex);
        try {
            emitter.completeWithError(ex);
        } catch (Exception ignored) {
        }
    }

    private void handleStreamComplete(
            SseEmitter emitter,
            ChatContext ctx,
            CompletionRequest request,
            String assistantContent,
            int tokensIn,
            int tokensOut,
            CheckLimitResult checkLimitResult
    ) {
        try {
            conversationPersistenceService.persistStreamCompletion(
                    ctx,
                    request,
                    assistantContent,
                    tokensIn,
                    tokensOut,
                    checkLimitResult
            );

            log.info(
                    "CHAT_COMPLETION_STREAM END convoId={} workspaceId={} projectId={} model={} tokensIn={} tokensOut={}",
                    ctx.conversationId(),
                    request.workspaceId(),
                    request.projectId(),
                    request.model(),
                    tokensIn,
                    tokensOut
            );
        } catch (Exception e) {
            log.error("CHAT_COMPLETION_STREAM PERSIST_FAILED convoId={}", ctx.conversationId(), e);
        } finally {
            emitter.complete();
        }
    }

    private ChatContext prepareChatContext(CompletionRequest request, String userId) {
        UUID workspaceId = request.workspaceId();
        UUID projectId = request.projectId();

        log.info("CHAT_CONTEXT_PREPARE START workspaceId={} projectId={}", workspaceId, projectId);

        workspaceAccessService.checkWorkspaceAccess(workspaceId);

        List<UUID> documentIds = workspaceAccessService.getDocumentIdsInProject(projectId);
        log.info(
                "CHAT_CONTEXT_PREPARE DOCUMENTS_LOADED projectId={} documentsCount={}",
                projectId,
                documentIds.size()
        );

        workspaceAccessService.validateRequestedDocuments(
                request.documentIds(),
                documentIds,
                projectId
        );

        String prompt = ragPromptBuilder.buildPrompt(request, documentIds);

        UUID conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID();

        List<CompletionLlmRouterRequest.Message> messages = request.conversationId() != null
                ? buildChatMemory(conversationId, request.memorySize())
                : null;

        if (messages != null) {
            log.info(
                    "CHAT_CONTEXT_PREPARE MEMORY_ATTACHED convoId={} messagesCount={}",
                    conversationId,
                    messages.size()
            );
        } else {
            log.info("CHAT_CONTEXT_PREPARE NEW_CONVERSATION convoId={}", conversationId);
        }

        UUID ownerUUID = UUID.fromString(userId);

        log.info(
                "CHAT_CONTEXT_PREPARE END convoId={} workspaceId={} projectId={} freeMode={} strictMode={}",
                conversationId,
                workspaceId,
                projectId,
                request.freeMode(),
                request.strictMode()
        );

        return new ChatContext(prompt, conversationId, projectId, workspaceId, messages, ownerUUID);
    }

    private void handleContextNotFoundStrictMode(
            SseEmitter emitter,
            CompletionRequest request,
            String userId
    ) {
        UUID conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID();

        UUID ownerId = UUID.fromString(userId);

        String assistantContent =
                "I couldn't find any relevant context in your documents for this question. " +
                        "Please adjust your filters, select different documents or upload more data.";

        log.info(
                "CHAT_COMPLETION_STREAM STRICT_NO_CONTEXT_RESPONSE convoId={} workspaceId={} projectId={}",
                conversationId,
                request.workspaceId(),
                request.projectId()
        );

        try {
            emitter.send(SseEmitter.event()
                    .name("info")
                    .data("No RAG context found for this query in strict mode."));

            emitter.send(SseEmitter.event()
                    .name("delta")
                    .data(assistantContent));

            emitter.send(SseEmitter.event()
                    .name("usage")
                    .data(new TokenUsage(0, 0)));

            emitter.send(SseEmitter.event()
                    .name("metadata")
                    .data(new StreamEventMetadata(conversationId)));

            emitter.send(SseEmitter.event()
                    .name("done")
                    .data("done"));
        } catch (Exception sendEx) {
            log.error("CHAT_COMPLETION_STREAM STRICT_NO_CONTEXT_SSE_FAILED convoId={}", conversationId, sendEx);
        } finally {
            try {
                conversationPersistenceService.persistStrictModeNoContext(
                        conversationId,
                        request,
                        assistantContent,
                        ownerId
                );

                log.info(
                        "CHAT_COMPLETION_STREAM STRICT_NO_CONTEXT_PERSISTED convoId={} workspaceId={} projectId={}",
                        conversationId,
                        request.workspaceId(),
                        request.projectId()
                );
            } catch (Exception persistEx) {
                log.error(
                        "CHAT_COMPLETION_STREAM STRICT_NO_CONTEXT_PERSIST_FAILED convoId={}",
                        conversationId,
                        persistEx
                );
            } finally {
                emitter.complete();
            }
        }
    }


    private CompletionResponse getCompletionResponse(
            String prompt,
            String model,
            List<CompletionLlmRouterRequest.Message> messages,
            UUID convoId
    ) {
        try {
            CompletionResponse response = llmRouterServiceClient.completion(
                    new CompletionLlmRouterRequest(prompt, model, messages)
            );

            log.info(
                    "CHAT_COMPLETION LLM_CALL_END convoId={} model={} tokensIn={} tokensOut={}",
                    convoId,
                    model,
                    response.tokensIn(),
                    response.tokensOut()
            );

            return response;
        } catch (Exception e) {
            log.error("CHAT_COMPLETION FAILED convoId={} model={}", convoId, model, e);
            throw new RuntimeException("CHAT_COMPLETION FAILED", e);
        }
    }

    private List<CompletionLlmRouterRequest.Message> buildChatMemory(UUID convoId, int memorySize) {
        List<ConversationMessage> history =
                conversationMessageRepository.findByConversationId(convoId, memorySize * 2);

        log.info(
                "CHAT_MEMORY LOAD convoId={} requestedPairs={} loadedMessages={}",
                convoId,
                memorySize,
                history.size()
        );

        return history.stream()
                .map(message -> new CompletionLlmRouterRequest.Message(
                        message.getRole().name().toLowerCase(),
                        message.getContent())
                )
                .toList();
    }


    public PagedResponse<ConversationMessageResponse> getMessages(
            UUID conversationId,
            String subject,
            PaginationRequest request
    ) {
        log.info(
                "CHAT_MESSAGES LIST_START convoId={} page={} size={}",
                conversationId,
                request.getPage(),
                request.getSize()
        );

        PagedResponse<ConversationMessageResponse> response =
                conversationCacheService.findMessages(conversationId, request);

        log.info(
                "CHAT_MESSAGES LIST_END convoId={} page={} size={} totalElements={}",
                conversationId,
                response.page(),
                response.size(),
                response.totalElements()
        );

        return response;
    }

    public List<UserConversationResponse> getUserConversations(String ownerId, UUID projectId) {
        UUID ownerUuid = UUID.fromString(ownerId);

        log.info("CHAT_USER_CONVERSATIONS START ownerId={}", ownerUuid);

        List<UserConversationResponse> cached =
                conversationCacheService.findUserConversations(ownerUuid, projectId);

        List<UserConversationResponse> result = cached.stream()
                .map(cm -> {
                    String cleaned = MarkdownCleaner.clean(cm.lastMessage());
                    String preview = cleaned.substring(0, Math.min(cleaned.length(), 100));
                    return new UserConversationResponse(
                            cm.conversationId(),
                            preview,
                            cm.lastMessageAt()
                    );
                })
                .toList();

        log.info(
                "CHAT_USER_CONVERSATIONS END ownerId={} conversationsCount={}",
                ownerUuid,
                result.size()
        );

        return result;
    }
}