package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.client.SubscriptionServiceClient;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.LogTokenUsageEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.LogTokenUsageEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationPersistenceServiceTest {

    @Mock
    private ConversationMessageRepository conversationMessageRepository;

    @Mock
    private LogTokenUsageEventProducer logTokenUsageEventProducer;

    @Mock
    private SubscriptionServiceClient subscriptionServiceClient;

    @Mock
    private ConversationCacheService conversationCacheService;

    @InjectMocks
    private ConversationPersistenceService conversationPersistenceService;

    @Captor
    private ArgumentCaptor<ConversationMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<LogTokenUsageEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<ReleaseReservationRequest> releaseCaptor;

    private UUID workspaceId;
    private UUID projectId;
    private UUID conversationId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    private CompletionRequest createRequest(String message, String model) {
        return new CompletionRequest(
                workspaceId,
                projectId,
                List.of(UUID.randomUUID()),
                conversationId,
                message,
                model,
                5,
                false,
                false
        );
    }

    private CheckLimitResult createCheckLimitResult(BigDecimal estimatedCost) {
        return CheckLimitResult.approved(
                estimatedCost,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(100)
        );
    }

    private void setupRepositorySaveWithIdGeneration() {
        when(conversationMessageRepository.save(any(ConversationMessage.class)))
                .thenAnswer(invocation -> {
                    ConversationMessage msg = invocation.getArgument(0);
                    msg.setId(UUID.randomUUID());
                    return msg;
                });
    }

    @Nested
    @DisplayName("persistSyncCompletion")
    class PersistSyncCompletionTests {

        @Test
        @DisplayName("Should save user and assistant messages with correct attributes")
        void persistSyncCompletion_savesUserAndAssistantMessages() {
            String userMessage = "What is wine tannin?";
            String assistantContent = "Tannin is a naturally occurring compound...";
            CompletionRequest request = createRequest(userMessage, "openai/gpt-4");
            CompletionResponse response = new CompletionResponse(assistantContent, 50, 100, "openai/gpt-4");
            CheckLimitResult checkLimitResult = createCheckLimitResult(BigDecimal.valueOf(0.005));

            UUID savedAssistantId = UUID.randomUUID();
            when(conversationMessageRepository.save(any(ConversationMessage.class)))
                    .thenAnswer(invocation -> {
                        ConversationMessage msg = invocation.getArgument(0);
                        msg.setId(UUID.randomUUID());
                        if (msg.getRole() == ConversationRole.ASSISTANT) {
                            msg.setId(savedAssistantId);
                        }
                        return msg;
                    });

            ConversationMessage result = conversationPersistenceService.persistSyncCompletion(
                    conversationId, request, response, ownerId, checkLimitResult
            );

            verify(conversationMessageRepository, times(2)).save(messageCaptor.capture());
            List<ConversationMessage> savedMessages = messageCaptor.getAllValues();

            ConversationMessage userMsg = savedMessages.get(0);
            assertThat(userMsg.getConversationId()).isEqualTo(conversationId);
            assertThat(userMsg.getContent()).isEqualTo(userMessage);
            assertThat(userMsg.getRole()).isEqualTo(ConversationRole.USER);
            assertThat(userMsg.getModelUsed()).isEqualTo("openai/gpt-4");
            assertThat(userMsg.getTokensCost()).isEqualTo(50);
            assertThat(userMsg.getOwnerId()).isEqualTo(ownerId);
            assertThat(userMsg.getProjectId()).isEqualTo(projectId);
            assertThat(userMsg.getWorkspaceId()).isEqualTo(workspaceId);

            // Verify assistant message
            ConversationMessage assistantMsg = savedMessages.get(1);
            assertThat(assistantMsg.getConversationId()).isEqualTo(conversationId);
            assertThat(assistantMsg.getContent()).isEqualTo(assistantContent);
            assertThat(assistantMsg.getRole()).isEqualTo(ConversationRole.ASSISTANT);
            assertThat(assistantMsg.getTokensCost()).isEqualTo(100);

            assertThat(result.getId()).isEqualTo(savedAssistantId);
        }

        @Test
        @DisplayName("Should produce token usage event with correct provider and model extracted")
        void persistSyncCompletion_producesTokenUsageEvent() {
            CompletionRequest request = createRequest("test", "anthropic/claude-3");
            CompletionResponse response = new CompletionResponse("response", 25, 75, "anthropic/claude-3");
            CheckLimitResult checkLimitResult = createCheckLimitResult(BigDecimal.valueOf(0.01));

            setupRepositorySaveWithIdGeneration();

            conversationPersistenceService.persistSyncCompletion(
                    conversationId, request, response, ownerId, checkLimitResult
            );

            verify(logTokenUsageEventProducer).produceLogTokenUsageEvent(eventCaptor.capture());
            LogTokenUsageEvent event = eventCaptor.getValue();

            assertThat(event.workspaceId()).isEqualTo(workspaceId.toString());
            assertThat(event.userId()).isEqualTo(ownerId.toString());
            assertThat(event.conversationId()).isEqualTo(conversationId.toString());
            assertThat(event.provider()).isEqualTo("anthropic");
            assertThat(event.model()).isEqualTo("claude-3");
            assertThat(event.sourceService()).isEqualTo("chat-service");
            assertThat(event.tokensIn()).isEqualTo(25);
            assertThat(event.tokensOut()).isEqualTo(75);
        }

        @Test
        @DisplayName("Should release reservation with correct amount")
        void persistSyncCompletion_releasesReservation() {
            CompletionRequest request = createRequest("test", "openai/gpt-4");
            CompletionResponse response = new CompletionResponse("response", 10, 20, "openai/gpt-4");
            BigDecimal estimatedCost = BigDecimal.valueOf(0.015);
            CheckLimitResult checkLimitResult = createCheckLimitResult(estimatedCost);

            setupRepositorySaveWithIdGeneration();

            conversationPersistenceService.persistSyncCompletion(
                    conversationId, request, response, ownerId, checkLimitResult
            );

            verify(subscriptionServiceClient).releaseReservation(releaseCaptor.capture());
            ReleaseReservationRequest releaseRequest = releaseCaptor.getValue();

            assertThat(releaseRequest.workspaceId()).isEqualTo(workspaceId);
            assertThat(releaseRequest.amount()).isEqualTo(estimatedCost);
        }
    }

    @Nested
    @DisplayName("persistStreamCompletion")
    class PersistStreamCompletionTests {

        @Test
        @DisplayName("Should save user and assistant messages from ChatContext")
        void persistStreamCompletion_savesMessages() {
            String userMessage = "Explain decanting";
            String assistantContent = "Decanting is the process of...";
            CompletionRequest request = createRequest(userMessage, "openai/gpt-4-turbo");
            ChatContext ctx = new ChatContext(
                    "prompt",
                    conversationId,
                    projectId,
                    workspaceId,
                    List.of(),
                    ownerId
            );
            CheckLimitResult checkLimitResult = createCheckLimitResult(BigDecimal.valueOf(0.02));

            setupRepositorySaveWithIdGeneration();

            conversationPersistenceService.persistStreamCompletion(
                    ctx, request, assistantContent, 100, 200, checkLimitResult
            );

            verify(conversationMessageRepository, times(2)).save(messageCaptor.capture());
            List<ConversationMessage> savedMessages = messageCaptor.getAllValues();

            ConversationMessage userMsg = savedMessages.get(0);
            assertThat(userMsg.getRole()).isEqualTo(ConversationRole.USER);
            assertThat(userMsg.getContent()).isEqualTo(userMessage);
            assertThat(userMsg.getTokensCost()).isEqualTo(100);

            ConversationMessage assistantMsg = savedMessages.get(1);
            assertThat(assistantMsg.getRole()).isEqualTo(ConversationRole.ASSISTANT);
            assertThat(assistantMsg.getContent()).isEqualTo(assistantContent);
            assertThat(assistantMsg.getTokensCost()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should produce token usage event and release reservation for stream")
        void persistStreamCompletion_producesEventAndReleasesReservation() {
            CompletionRequest request = createRequest("test", "openai/gpt-4");
            ChatContext ctx = new ChatContext(
                    "prompt",
                    conversationId,
                    projectId,
                    workspaceId,
                    List.of(),
                    ownerId
            );
            BigDecimal estimatedCost = BigDecimal.valueOf(0.03);
            CheckLimitResult checkLimitResult = createCheckLimitResult(estimatedCost);

            setupRepositorySaveWithIdGeneration();

            conversationPersistenceService.persistStreamCompletion(
                    ctx, request, "response", 50, 150, checkLimitResult
            );

            verify(logTokenUsageEventProducer).produceLogTokenUsageEvent(any(LogTokenUsageEvent.class));
            verify(subscriptionServiceClient).releaseReservation(releaseCaptor.capture());

            assertThat(releaseCaptor.getValue().workspaceId()).isEqualTo(workspaceId);
            assertThat(releaseCaptor.getValue().amount()).isEqualTo(estimatedCost);
        }
    }

    @Nested
    @DisplayName("persistStrictModeNoContext")
    class PersistStrictModeNoContextTests {

        @Test
        @DisplayName("Should save messages with zero token cost when no context found")
        void persistStrictModeNoContext_savesMessagesWithZeroTokens() {
            String userMessage = "Question without context";
            String assistantContent = "I couldn't find relevant information in your documents.";
            CompletionRequest request = createRequest(userMessage, "openai/gpt-4");

            UUID savedAssistantId = UUID.randomUUID();
            when(conversationMessageRepository.save(any(ConversationMessage.class)))
                    .thenAnswer(invocation -> {
                        ConversationMessage msg = invocation.getArgument(0);
                        msg.setId(UUID.randomUUID());
                        if (msg.getRole() == ConversationRole.ASSISTANT) {
                            msg.setId(savedAssistantId);
                        }
                        return msg;
                    });

            ConversationMessage result = conversationPersistenceService.persistStrictModeNoContext(
                    conversationId, request, assistantContent, ownerId
            );

            verify(conversationMessageRepository, times(2)).save(messageCaptor.capture());
            List<ConversationMessage> savedMessages = messageCaptor.getAllValues();

            assertThat(savedMessages.get(0).getTokensCost()).isZero();
            assertThat(savedMessages.get(1).getTokensCost()).isZero();

            assertThat(result.getId()).isEqualTo(savedAssistantId);
        }

        @Test
        @DisplayName("Should NOT produce token usage event or release reservation for strict mode no context")
        void persistStrictModeNoContext_doesNotProduceEventOrRelease() {
            CompletionRequest request = createRequest("test", "openai/gpt-4");

            setupRepositorySaveWithIdGeneration();

            conversationPersistenceService.persistStrictModeNoContext(
                    conversationId, request, "No context response", ownerId
            );

            verify(logTokenUsageEventProducer, never()).produceLogTokenUsageEvent(any());
            verify(subscriptionServiceClient, never()).releaseReservation(any());
        }
    }
}
