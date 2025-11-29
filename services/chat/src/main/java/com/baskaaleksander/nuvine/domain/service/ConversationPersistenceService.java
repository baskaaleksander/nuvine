package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.ChatContext;
import com.baskaaleksander.nuvine.application.dto.CompletionRequest;
import com.baskaaleksander.nuvine.application.dto.CompletionResponse;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationPersistenceService {

    private final ConversationMessageRepository conversationMessageRepository;

    public ConversationMessage persistSyncCompletion(
            UUID conversationId,
            CompletionRequest request,
            CompletionResponse completion,
            UUID ownerId
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
                .modelUsed(request.model())
                .tokensCost(completion.tokensIn())
                .ownerId(ownerId)
                .cost(0)
                .build();

        conversationMessageRepository.save(userMessage);

        ConversationMessage assistantMessage = ConversationMessage.builder()
                .conversationId(conversationId)
                .content(completion.content())
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

        return savedAssistant;
    }

    public void persistStreamCompletion(
            ChatContext ctx,
            CompletionRequest request,
            String assistantContent,
            int tokensIn,
            int tokensOut
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
                .role(ConversationRole.ASSISTANT)
                .modelUsed(request.model())
                .ownerId(ctx.ownerId())
                .tokensCost(tokensOut)
                .cost(0)
                .build();

        conversationMessageRepository.save(assistantMessage);

        log.info(
                "CONVERSATION_PERSIST_STREAM END convoId={} model={} userMsgId={} assistantMsgId={}",
                ctx.conversationId(),
                request.model(),
                userMessage.getId(),
                assistantMessage.getId()
        );
    }

    public void persistStrictModeNoContext(
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
                .modelUsed(request.model())
                .ownerId(ownerId)
                .tokensCost(0)
                .cost(0)
                .build();

        conversationMessageRepository.save(assistantMessage);

        log.info(
                "CONVERSATION_PERSIST_STRICT_NO_CONTEXT END convoId={} model={} userMsgId={} assistantMsgId={}",
                conversationId,
                request.model(),
                userMessage.getId(),
                assistantMessage.getId()
        );
    }
}