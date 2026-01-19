package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.CompletionRequest;
import com.baskaaleksander.nuvine.application.dto.ConversationMessageResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.application.dto.UserConversationResponse;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChatControllerIT extends BaseControllerIntegrationTest {

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    @Autowired
    private TestDataBuilder testDataBuilder;

    private JwtTestUtils jwtTestUtils;
    private UUID userId;
    private UUID workspaceId;
    private UUID projectId;
    private String validJwt;

    @BeforeEach
    void setUp() {
        conversationMessageRepository.deleteAll();

        jwtTestUtils = new JwtTestUtils("http://localhost:" + wireMockServer.port() + "/realms/nuvine");
        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        validJwt = jwtTestUtils.generateJwt(userId, "test@example.com");
    }

    @Nested
    @DisplayName("POST /api/v1/chat/completions (sync)")
    class CompletionsEndpoint {

        @Test
        @DisplayName("Authenticated with approved limit returns response")
        void authenticated_withApprovedLimit_returnsResponse() {
            // given
            UUID documentId = UUID.randomUUID();
            setupSuccessfulCompletionStubs(documentId);

            CompletionRequest request = new CompletionRequest(
                    workspaceId,
                    projectId,
                    List.of(documentId),
                    null, // new conversation
                    "What is the meaning of life?",
                    "openai/gpt-4",
                    5,
                    false,
                    false
            );

            HttpHeaders headers = authHeaders(validJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CompletionRequest> entity = new HttpEntity<>(request, headers);

            // when
            ResponseEntity<ConversationMessageResponse> response = restTemplate.exchange(
                    "/api/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    ConversationMessageResponse.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).isEqualTo("The meaning of life is 42.");
            assertThat(response.getBody().role()).isEqualTo(ConversationRole.ASSISTANT);
            assertThat(response.getBody().conversationId()).isNotNull();
        }

        @Test
        @DisplayName("Unauthenticated returns 401")
        void unauthenticated_returns401() {
            // given
            CompletionRequest request = new CompletionRequest(
                    workspaceId,
                    projectId,
                    null,
                    null,
                    "Hello",
                    "openai/gpt-4",
                    5,
                    false,
                    false
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CompletionRequest> entity = new HttpEntity<>(request, headers);

            // when
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Limit exceeded returns 403")
        void limitExceeded_returns403() {
            // given
            UUID documentId = UUID.randomUUID();
            wireMockStubs.stubWorkspaceAccessGranted(workspaceId);
            wireMockStubs.stubProjectDocumentIds(projectId, List.of(documentId));
            wireMockStubs.stubVectorSearchWithResults(List.of(
                    new WireMockStubs.VectorMatch(documentId, 1, 0, 100, "Some context", 0.9f)
            ));
            wireMockStubs.stubSubscriptionCheckLimitRejected(
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(50),
                    BigDecimal.valueOf(0.01),
                    BigDecimal.valueOf(100)
            );

            CompletionRequest request = new CompletionRequest(
                    workspaceId,
                    projectId,
                    List.of(documentId),
                    null,
                    "Question",
                    "openai/gpt-4",
                    5,
                    false,
                    false
            );

            HttpHeaders headers = authHeaders(validJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CompletionRequest> entity = new HttpEntity<>(request, headers);

            // when
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Strict mode with no context returns strict mode message")
        void strictMode_withNoContext_returnsStrictModeMessage() {
            // given
            UUID documentId = UUID.randomUUID();
            wireMockStubs.stubWorkspaceAccessGranted(workspaceId);
            wireMockStubs.stubProjectDocumentIds(projectId, List.of(documentId));
            wireMockStubs.stubVectorSearchEmpty();

            CompletionRequest request = new CompletionRequest(
                    workspaceId,
                    projectId,
                    List.of(documentId),
                    null,
                    "Question about nothing",
                    "openai/gpt-4",
                    5,
                    true, // strictMode
                    false
            );

            HttpHeaders headers = authHeaders(validJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CompletionRequest> entity = new HttpEntity<>(request, headers);

            // when
            ResponseEntity<ConversationMessageResponse> response = restTemplate.exchange(
                    "/api/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    ConversationMessageResponse.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).contains("couldn't find any relevant context");
        }

        @Test
        @DisplayName("Existing conversation with different owner returns 403")
        void existingConversation_withDifferentOwner_returns403() {
            // given
            UUID conversationId = UUID.randomUUID();
            UUID otherOwnerId = UUID.randomUUID();

            // Create a message owned by a different user
            ConversationMessage existingMessage = ConversationMessage.builder()
                    .conversationId(conversationId)
                    .content("Existing message")
                    .role(ConversationRole.USER)
                    .modelUsed("none")
                    .tokensCost(0)
                    .ownerId(otherOwnerId) // different owner
                    .projectId(projectId)
                    .workspaceId(workspaceId)
                    .cost(0.0)
                    .build();
            conversationMessageRepository.save(existingMessage);

            CompletionRequest request = new CompletionRequest(
                    workspaceId,
                    projectId,
                    null,
                    conversationId, // existing conversation
                    "My question",
                    "openai/gpt-4",
                    5,
                    false,
                    false
            );

            HttpHeaders headers = authHeaders(validJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CompletionRequest> entity = new HttpEntity<>(request, headers);

            // when
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chat")
    class GetUserConversationsEndpoint {

        @Test
        @DisplayName("Authenticated returns user's conversations")
        void authenticated_returnsUserConversations() {
            // given
            UUID conversation1 = UUID.randomUUID();
            UUID conversation2 = UUID.randomUUID();

            createMessage(conversation1, "Hello from conv1", ConversationRole.USER);
            createMessage(conversation1, "Response from conv1", ConversationRole.ASSISTANT);

            createMessage(conversation2, "Hello from conv2", ConversationRole.USER);

            HttpHeaders headers = authHeaders(validJwt);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // when
            ResponseEntity<List<UserConversationResponse>> response = restTemplate.exchange(
                    "/api/v1/chat?projectId=" + projectId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("Unauthenticated returns 401")
        void unauthenticated_returns401() {
            // when
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/v1/chat?projectId=" + projectId,
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chat/{conversationId}")
    class GetMessagesEndpoint {

        @Test
        @DisplayName("As owner returns paged messages")
        void asOwner_returnsPagedMessages() {
            // given
            UUID conversationId = UUID.randomUUID();

            createMessage(conversationId, "User message 1", ConversationRole.USER);
            createMessage(conversationId, "Assistant response 1", ConversationRole.ASSISTANT);
            createMessage(conversationId, "User message 2", ConversationRole.USER);
            createMessage(conversationId, "Assistant response 2", ConversationRole.ASSISTANT);

            HttpHeaders headers = authHeaders(validJwt);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // when
            ResponseEntity<PagedResponse<ConversationMessageResponse>> response = restTemplate.exchange(
                    "/api/v1/chat/" + conversationId + "?page=0&size=2",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).hasSize(2);
            assertThat(response.getBody().totalElements()).isEqualTo(4);
            assertThat(response.getBody().totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("Not owner returns 403")
        void notOwner_returns403() {
            // given
            UUID conversationId = UUID.randomUUID();
            UUID otherOwnerId = UUID.randomUUID();

            ConversationMessage message = ConversationMessage.builder()
                    .conversationId(conversationId)
                    .content("Someone else's message")
                    .role(ConversationRole.USER)
                    .modelUsed("none")
                    .tokensCost(0)
                    .ownerId(otherOwnerId) // different owner
                    .projectId(projectId)
                    .workspaceId(workspaceId)
                    .cost(0.0)
                    .build();
            conversationMessageRepository.save(message);

            HttpHeaders headers = authHeaders(validJwt);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/chat/" + conversationId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Non-existent conversation returns 500 or error")
        void nonExistentConversation_returnsError() {
            // given
            UUID nonExistentConversationId = UUID.randomUUID();

            HttpHeaders headers = authHeaders(validJwt);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // when
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/chat/" + nonExistentConversationId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // then - the chatAccess evaluation throws NoSuchElementException when no messages exist
            assertThat(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()).isTrue();
        }
    }

    private void setupSuccessfulCompletionStubs(UUID documentId) {
        wireMockStubs.stubWorkspaceAccessGranted(workspaceId);
        wireMockStubs.stubProjectDocumentIds(projectId, List.of(documentId));
        wireMockStubs.stubVectorSearchWithResults(List.of(
                new WireMockStubs.VectorMatch(documentId, 1, 0, 100, "The answer is 42.", 0.95f)
        ));
        wireMockStubs.stubSubscriptionCheckLimitApproved(
                BigDecimal.valueOf(0.01),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(0),
                BigDecimal.valueOf(100)
        );
        wireMockStubs.stubSubscriptionReleaseReservation();
        wireMockStubs.stubLlmRouterCompletion("The meaning of life is 42.", 100, 50, "gpt-4");
    }

    private void createMessage(UUID conversationId, String content, ConversationRole role) {
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(conversationId)
                .content(content)
                .role(role)
                .modelUsed(role == ConversationRole.ASSISTANT ? "openai/gpt-4" : "none")
                .tokensCost(role == ConversationRole.ASSISTANT ? 100 : 0)
                .ownerId(userId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(role == ConversationRole.ASSISTANT ? 0.001 : 0.0)
                .build();
        conversationMessageRepository.save(message);
    }
}
