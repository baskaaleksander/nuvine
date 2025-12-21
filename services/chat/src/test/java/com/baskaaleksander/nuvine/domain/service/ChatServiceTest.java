package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.ConversationMessageMapper;
import com.baskaaleksander.nuvine.domain.exception.ContextNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.RequestLimitExceededException;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.client.LlmRouterServiceClient;
import com.baskaaleksander.nuvine.infrastructure.client.SubscriptionServiceClient;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private LlmRouterServiceClient llmRouterServiceClient;

    @Mock
    private ConversationMessageRepository conversationMessageRepository;

    @Mock
    private ConversationMessageMapper mapper;

    @Mock
    private WebClient llmRouterWebClient;

    @Mock
    private SubscriptionServiceClient subscriptionServiceClient;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private RagPromptBuilder ragPromptBuilder;

    @Mock
    private ConversationPersistenceService conversationPersistenceService;

    @Mock
    private TokenCountingService tokenCountingService;

    @InjectMocks
    private ChatService chatService;

    private UUID workspaceId;
    private UUID projectId;
    private UUID conversationId;
    private String userId;
    private List<UUID> documentIds;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID().toString();
        documentIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    }

    private CompletionRequest createRequest(String message, String model, boolean strictMode, boolean freeMode) {
        return new CompletionRequest(
                workspaceId,
                projectId,
                documentIds,
                conversationId,
                message,
                model,
                5,
                strictMode,
                freeMode
        );
    }

    private CheckLimitResult createApprovedLimit(BigDecimal cost) {
        return CheckLimitResult.approved(cost, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.valueOf(100));
    }

    private CheckLimitResult createRejectedLimit() {
        return CheckLimitResult.rejected(BigDecimal.TEN, BigDecimal.valueOf(100), BigDecimal.ONE, BigDecimal.valueOf(100));
    }

    @Nested
    @DisplayName("completion - sync")
    class CompletionTests {

        @Test
        @DisplayName("Should successfully complete chat and return response")
        void completion_success_returnsResponse() {
            String userMessage = "Tell me about wine tannins";
            String assistantContent = "Tannins are polyphenolic compounds...";
            CompletionRequest request = createRequest(userMessage, "openai/gpt-4", false, false);

            String prompt = "Processed prompt with context";
            when(workspaceAccessService.getDocumentIdsInProject(projectId)).thenReturn(documentIds);
            when(ragPromptBuilder.buildPrompt(eq(request), eq(documentIds))).thenReturn(prompt);
            when(tokenCountingService.count(prompt)).thenReturn(100);
            when(subscriptionServiceClient.checkLimit(any(CheckLimitRequest.class)))
                    .thenReturn(createApprovedLimit(BigDecimal.valueOf(0.01)));

            CompletionResponse llmResponse = new CompletionResponse(assistantContent, 50, 100, "openai/gpt-4");
            when(llmRouterServiceClient.completion(any(CompletionLlmRouterRequest.class)))
                    .thenReturn(llmResponse);

            UUID assistantMsgId = UUID.randomUUID();
            ConversationMessage savedAssistant = ConversationMessage.builder()
                    .id(assistantMsgId)
                    .conversationId(conversationId)
                    .content(assistantContent)
                    .role(ConversationRole.ASSISTANT)
                    .modelUsed("openai/gpt-4")
                    .tokensCost(100)
                    .ownerId(UUID.fromString(userId))
                    .createdAt(Instant.now())
                    .build();

            when(conversationPersistenceService.persistSyncCompletion(
                    any(UUID.class), eq(request), eq(llmResponse), any(UUID.class), any(CheckLimitResult.class)
            )).thenReturn(savedAssistant);

            ConversationMessageResponse result = chatService.completion(request, userId);

            assertThat(result.id()).isEqualTo(assistantMsgId);
            assertThat(result.content()).isEqualTo(assistantContent);
            assertThat(result.role()).isEqualTo(ConversationRole.ASSISTANT);
            assertThat(result.conversationId()).isEqualTo(conversationId);

            verify(workspaceAccessService).checkWorkspaceAccess(workspaceId);
            verify(workspaceAccessService).validateRequestedDocuments(documentIds, documentIds, projectId);
            verify(llmRouterServiceClient).completion(any(CompletionLlmRouterRequest.class));
            verify(conversationPersistenceService).persistSyncCompletion(
                    any(), eq(request), eq(llmResponse), any(), any()
            );
        }

        @Test
        @DisplayName("Should throw RequestLimitExceededException when limit is not approved")
        void completion_limitExceeded_throwsException() {
            CompletionRequest request = createRequest("test", "openai/gpt-4", false, false);

            when(workspaceAccessService.getDocumentIdsInProject(projectId)).thenReturn(documentIds);
            when(ragPromptBuilder.buildPrompt(eq(request), eq(documentIds))).thenReturn("prompt");
            when(tokenCountingService.count(anyString())).thenReturn(100);
            when(subscriptionServiceClient.checkLimit(any(CheckLimitRequest.class)))
                    .thenReturn(createRejectedLimit());

            assertThatThrownBy(() -> chatService.completion(request, userId))
                    .isInstanceOf(RequestLimitExceededException.class)
                    .hasMessage("Limit exceeded exception");

            verify(llmRouterServiceClient, never()).completion(any());
            verify(conversationPersistenceService, never()).persistSyncCompletion(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should handle strict mode with no context by returning fallback message")
        void completion_strictModeNoContext_returnsFallbackResponse() {
            CompletionRequest request = createRequest("test", "openai/gpt-4", true, false);

            when(workspaceAccessService.getDocumentIdsInProject(projectId)).thenReturn(documentIds);
            when(ragPromptBuilder.buildPrompt(eq(request), eq(documentIds)))
                    .thenThrow(new ContextNotFoundException("Context not found"));

            UUID assistantMsgId = UUID.randomUUID();
            String fallbackContent = "I couldn't find any relevant context in your documents for this question. " +
                    "Please adjust your filters, select different documents or upload more data.";
            ConversationMessage savedAssistant = ConversationMessage.builder()
                    .id(assistantMsgId)
                    .conversationId(conversationId)
                    .content(fallbackContent)
                    .role(ConversationRole.ASSISTANT)
                    .modelUsed("openai/gpt-4")
                    .tokensCost(0)
                    .ownerId(UUID.fromString(userId))
                    .createdAt(Instant.now())
                    .build();

            when(conversationPersistenceService.persistStrictModeNoContext(
                    any(UUID.class), eq(request), anyString(), any(UUID.class)
            )).thenReturn(savedAssistant);

            ConversationMessageResponse result = chatService.completion(request, userId);

            assertThat(result.id()).isEqualTo(assistantMsgId);
            assertThat(result.content()).contains("couldn't find any relevant context");
            assertThat(result.tokensCost()).isZero();

            verify(llmRouterServiceClient, never()).completion(any());
            verify(conversationPersistenceService).persistStrictModeNoContext(
                    any(), eq(request), anyString(), any()
            );
        }

        @Test
        @DisplayName("Should include conversation history when conversationId is provided")
        void completion_withConversationHistory_includesMemory() {
            CompletionRequest request = createRequest("follow up question", "openai/gpt-4", false, false);

            List<ConversationMessage> history = List.of(
                    ConversationMessage.builder()
                            .role(ConversationRole.USER)
                            .content("previous question")
                            .build(),
                    ConversationMessage.builder()
                            .role(ConversationRole.ASSISTANT)
                            .content("previous answer")
                            .build()
            );

            when(workspaceAccessService.getDocumentIdsInProject(projectId)).thenReturn(documentIds);
            when(ragPromptBuilder.buildPrompt(eq(request), eq(documentIds))).thenReturn("prompt");
            when(conversationMessageRepository.findByConversationId(conversationId, 10))
                    .thenReturn(history);
            when(tokenCountingService.count(anyString())).thenReturn(100);
            when(subscriptionServiceClient.checkLimit(any(CheckLimitRequest.class)))
                    .thenReturn(createApprovedLimit(BigDecimal.valueOf(0.01)));

            CompletionResponse llmResponse = new CompletionResponse("response", 50, 100, "openai/gpt-4");
            when(llmRouterServiceClient.completion(any(CompletionLlmRouterRequest.class)))
                    .thenReturn(llmResponse);

            ConversationMessage savedAssistant = ConversationMessage.builder()
                    .id(UUID.randomUUID())
                    .conversationId(conversationId)
                    .content("response")
                    .role(ConversationRole.ASSISTANT)
                    .modelUsed("openai/gpt-4")
                    .tokensCost(100)
                    .ownerId(UUID.fromString(userId))
                    .createdAt(Instant.now())
                    .build();

            when(conversationPersistenceService.persistSyncCompletion(any(), any(), any(), any(), any()))
                    .thenReturn(savedAssistant);

            chatService.completion(request, userId);

            verify(conversationMessageRepository).findByConversationId(conversationId, 10);
            verify(llmRouterServiceClient).completion(argThat(req ->
                    req.messages() != null && req.messages().size() == 2
            ));
        }
    }

    @Nested
    @DisplayName("getMessages")
    class GetMessagesTests {

        @Test
        @DisplayName("Should return paginated messages for conversation")
        void getMessages_returnsPaginatedResponse() {
            PaginationRequest paginationRequest = new PaginationRequest();
            paginationRequest.setPage(0);
            paginationRequest.setSize(10);
            paginationRequest.setSortField("createdAt");
            paginationRequest.setDirection(Sort.Direction.DESC);

            ConversationMessage msg1 = ConversationMessage.builder()
                    .id(UUID.randomUUID())
                    .conversationId(conversationId)
                    .content("Hello")
                    .role(ConversationRole.USER)
                    .build();
            ConversationMessage msg2 = ConversationMessage.builder()
                    .id(UUID.randomUUID())
                    .conversationId(conversationId)
                    .content("Hi there")
                    .role(ConversationRole.ASSISTANT)
                    .build();

            Page<ConversationMessage> page = new PageImpl<>(List.of(msg1, msg2));

            when(conversationMessageRepository.findAllByConversationId(eq(conversationId), any(Pageable.class)))
                    .thenReturn(page);

            ConversationMessageResponse response1 = new ConversationMessageResponse(
                    msg1.getId(), conversationId, "Hello", ConversationRole.USER, "gpt-4", 10, UUID.randomUUID(), Instant.now()
            );
            ConversationMessageResponse response2 = new ConversationMessageResponse(
                    msg2.getId(), conversationId, "Hi there", ConversationRole.ASSISTANT, "gpt-4", 20, UUID.randomUUID(), Instant.now()
            );

            when(mapper.toResponse(msg1)).thenReturn(response1);
            when(mapper.toResponse(msg2)).thenReturn(response2);

            PagedResponse<ConversationMessageResponse> result = chatService.getMessages(
                    conversationId, userId, paginationRequest
            );

            assertThat(result.content()).hasSize(2);
            assertThat(result.content()).extracting("content").containsExactly("Hello", "Hi there");
        }

        @Test
        @DisplayName("Should return empty page when no messages exist")
        void getMessages_noMessages_returnsEmptyPage() {
            PaginationRequest paginationRequest = new PaginationRequest();
            paginationRequest.setPage(0);
            paginationRequest.setSize(10);

            Page<ConversationMessage> emptyPage = Page.empty();

            when(conversationMessageRepository.findAllByConversationId(eq(conversationId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PagedResponse<ConversationMessageResponse> result = chatService.getMessages(
                    conversationId, userId, paginationRequest
            );

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getUserConversations")
    class GetUserConversationsTests {

        @Test
        @DisplayName("Should return user conversations with cleaned preview")
        void getUserConversations_returnsConversationsWithCleanedPreview() {
            UUID convo1Id = UUID.randomUUID();
            UUID convo2Id = UUID.randomUUID();
            Instant now = Instant.now();

            List<UserConversationResponse> responses = List.of(
                    new UserConversationResponse(convo1Id, "**Bold** markdown text in message", now),
                    new UserConversationResponse(convo2Id, "Plain text message", now.minusSeconds(60))
            );

            when(conversationMessageRepository.findUserConversations(any(UUID.class), eq(projectId)))
                    .thenReturn(responses);

            List<UserConversationResponse> result = chatService.getUserConversations(userId, projectId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).lastMessage()).doesNotContain("**");
            assertThat(result.get(0).conversationId()).isEqualTo(convo1Id);
        }

        @Test
        @DisplayName("Should truncate long preview to 100 characters")
        void getUserConversations_longMessage_truncatesPreview() {
            String longMessage = "A".repeat(200);
            UUID convoId = UUID.randomUUID();

            List<UserConversationResponse> responses = List.of(
                    new UserConversationResponse(convoId, longMessage, Instant.now())
            );

            when(conversationMessageRepository.findUserConversations(any(UUID.class), eq(projectId)))
                    .thenReturn(responses);

            List<UserConversationResponse> result = chatService.getUserConversations(userId, projectId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).lastMessage()).hasSize(100);
        }

        @Test
        @DisplayName("Should return empty list when user has no conversations")
        void getUserConversations_noConversations_returnsEmptyList() {
            when(conversationMessageRepository.findUserConversations(any(UUID.class), eq(projectId)))
                    .thenReturn(List.of());

            List<UserConversationResponse> result = chatService.getUserConversations(userId, projectId);

            assertThat(result).isEmpty();
        }
    }
}
