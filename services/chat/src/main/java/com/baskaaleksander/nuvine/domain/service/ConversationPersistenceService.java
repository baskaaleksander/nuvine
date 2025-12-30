package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.client.SubscriptionServiceClient;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.LogTokenUsageEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationPersistenceService {

    private final ConversationMessageRepository conversationMessageRepository;
    private final LogTokenUsageEventProducer logTokenUsageEventProducer;
    private final SubscriptionServiceClient subscriptionServiceClient;
    private final ConversationCacheService conversationCacheService;

    public ConversationMessage persistSyncCompletion(
            UUID conversationId,
            CompletionRequest request,
            CompletionResponse completion,
            UUID ownerId,
            CheckLimitResult checkLimitResult
    ) {
        log.info(
                "CONVERSATION_PERSIST_SYNC START convoId={} model={} tokensIn={} tokensOut={}",
                conversationId,
                request.model(),
                completion.tokensIn(),
                completion.tokensOut()
        );

        ConversationMessage userMessage = ConversationMessage.builder()
                .conversationId(conversationId)
                .content(request.message())
                .role(ConversationRole.USER)
                .projectId(request.projectId())
                .workspaceId(request.workspaceId())
                .modelUsed(request.model())
                .tokensCost(completion.tokensIn())
                .ownerId(ownerId)
                .cost(0)
                .build();

        conversationMessageRepository.save(userMessage);

        ConversationMessage assistantMessage = ConversationMessage.builder()
                .conversationId(conversationId)
                .content(completion.content())
                .projectId(request.projectId())
                .workspaceId(request.workspaceId())
                .role(ConversationRole.ASSISTANT)
                .modelUsed(request.model())
                .tokensCost(completion.tokensOut())
                .ownerId(ownerId)
                .cost(0)
                .build();

        ConversationMessage savedAssistant = conversationMessageRepository.save(assistantMessage);

        log.info(
                "CONVERSATION_PERSIST_SYNC END convoId={} model={} userMsgId={} assistantMsgId={}",
                conversationId,
                request.model(),
                userMessage.getId(),
                savedAssistant.getId()
        );

        String provider = request.model().split("/")[0];
        String model = request.model().split("/")[1];

        logTokenUsageEventProducer.produceLogTokenUsageEvent(
                new LogTokenUsageEvent(
                        request.workspaceId().toString(),
                        ownerId.toString(),
                        conversationId.toString(),
                        userMessage.getId().toString(),
                        model,
                        provider,
                        "chat-service",
                        completion.tokensIn(),
                        completion.tokensOut(),
                        Instant.now()
                )
        );

        subscriptionServiceClient.releaseReservation(
                new ReleaseReservationRequest(
                        request.workspaceId(),
                        checkLimitResult.estimatedCost()
                )
        );

        conversationCacheService.evictAfterNewMessage(ownerId, request.projectId(), conversationId);

        return savedAssistant;
    }

    public void persistStreamCompletion(
            ChatContext ctx,
            CompletionRequest request,
            String assistantContent,
            int tokensIn,
            int tokensOut,
            CheckLimitResult checkLimitResult
    ) {
        log.info(
                "CONVERSATION_PERSIST_STREAM START convoId={} model={} tokensIn={} tokensOut={}",
                ctx.conversationId(),
                request.model(),
                tokensIn,
                tokensOut
        );

        ConversationMessage userMessage = ConversationMessage.builder()
                .conversationId(ctx.conversationId())
                .content(request.message())
                .projectId(ctx.projectId())
                .workspaceId(ctx.workspaceId())
                .role(ConversationRole.USER)
                .modelUsed(request.model())
                .ownerId(ctx.ownerId())
                .tokensCost(tokensIn)
                .cost(0)
                .build();

        conversationMessageRepository.save(userMessage);

        ConversationMessage assistantMessage = ConversationMessage.builder()
                .conversationId(ctx.conversationId())
                .content(assistantContent)
                .projectId(ctx.projectId())
                .workspaceId(ctx.workspaceId())
                .role(ConversationRole.ASSISTANT)
                .modelUsed(request.model())
                .ownerId(ctx.ownerId())
                .tokensCost(tokensOut)
                .cost(0)
                .build();

        conversationMessageRepository.save(assistantMessage);

        String provider = request.model().split("/")[0];
        String model = request.model().split("/")[1];

        logTokenUsageEventProducer.produceLogTokenUsageEvent(
                new LogTokenUsageEvent(
                        request.workspaceId().toString(),
                        ctx.ownerId().toString(),
                        ctx.conversationId().toString(),
                        userMessage.getId().toString(),
                        model,
                        provider,
                        "chat-service",
                        tokensIn,
                        tokensOut,
                        Instant.now()
                )
        );

        subscriptionServiceClient.releaseReservation(
                new ReleaseReservationRequest(
                        request.workspaceId(),
                        checkLimitResult.estimatedCost()
                )
        );

        conversationCacheService.evictAfterNewMessage(ctx.ownerId(), request.projectId(), ctx.conversationId());

        log.info(
                "CONVERSATION_PERSIST_STREAM END convoId={} model={} userMsgId={} assistantMsgId={}",
                ctx.conversationId(),
                request.model(),
                userMessage.getId(),
                assistantMessage.getId()
        );
    }

    public ConversationMessage persistStrictModeNoContext(
            UUID conversationId,
            CompletionRequest request,
            String assistantContent,
            UUID ownerId
    ) {
        log.info(
                "CONVERSATION_PERSIST_STRICT_NO_CONTEXT START convoId={} model={}",
                conversationId,
                request.model()
        );

        ConversationMessage userMessage = ConversationMessage.builder()
                .conversationId(conversationId)
                .content(request.message())
                .role(ConversationRole.USER)
                .projectId(request.projectId())
                .workspaceId(request.workspaceId())
                .modelUsed(request.model())
                .ownerId(ownerId)
                .tokensCost(0)
                .cost(0)
                .build();

        conversationMessageRepository.save(userMessage);

        ConversationMessage assistantMessage = ConversationMessage.builder()
                .conversationId(conversationId)
                .content(assistantContent)
                .role(ConversationRole.ASSISTANT)
                .projectId(request.projectId())
                .workspaceId(request.workspaceId())
                .modelUsed(request.model())
                .ownerId(ownerId)
                .tokensCost(0)
                .cost(0)
                .build();

        ConversationMessage savedAssistant = conversationMessageRepository.save(assistantMessage);

        log.info(
                "CONVERSATION_PERSIST_STRICT_NO_CONTEXT END convoId={} model={} userMsgId={} assistantMsgId={}",
                conversationId,
                request.model(),
                userMessage.getId(),
                savedAssistant.getId()
        );

        conversationCacheService.evictAfterNewMessage(ownerId, request.projectId(), conversationId);

        return savedAssistant;
    }
}