package com.baskaaleksander.nuvine.integration.repository;

import com.baskaaleksander.nuvine.application.dto.UserConversationResponse;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import com.baskaaleksander.nuvine.integration.base.BaseRepositoryIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMessageRepositoryIT extends BaseRepositoryIntegrationTest {

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    private UUID ownerId;
    private UUID projectId;
    private UUID workspaceId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        truncateTables("conversation_message");
        ownerId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Save message persists to database")
    void saveMessage_persistsToDatabase() {
        // given
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(conversationId)
                .content("Hello, this is a test message")
                .role(ConversationRole.USER)
                .modelUsed("none")
                .tokensCost(0)
                .ownerId(ownerId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(0.0)
                .build();

        // when
        ConversationMessage saved = conversationMessageRepository.saveAndFlush(message);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getConversationId()).isEqualTo(conversationId);
        assertThat(saved.getContent()).isEqualTo("Hello, this is a test message");
        assertThat(saved.getRole()).isEqualTo(ConversationRole.USER);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByConversationId with limit returns limited messages")
    void findByConversationId_withLimit_returnsLimitedMessages() {
        // given
        createMessages(5);

        // when
        List<ConversationMessage> messages = conversationMessageRepository.findByConversationId(conversationId, 3);

        // then
        assertThat(messages).hasSize(3);
        // Should be ordered by createdAt ASC
        for (int i = 0; i < messages.size() - 1; i++) {
            assertThat(messages.get(i).getCreatedAt())
                    .isBeforeOrEqualTo(messages.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("findByConversationId empty conversation returns empty list")
    void findByConversationId_emptyConversation_returnsEmptyList() {
        // when
        List<ConversationMessage> messages = conversationMessageRepository.findByConversationId(UUID.randomUUID(), 10);

        // then
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("findAllByConversationId with pagination returns paged results")
    void findAllByConversationId_withPagination_returnsPagedResults() {
        // given
        createMessages(10);
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "createdAt"));

        // when
        Page<ConversationMessage> page = conversationMessageRepository.findAllByConversationId(conversationId, pageRequest);

        // then
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("findUserConversations returns latest message per conversation")
    void findUserConversations_returnsLatestMessagePerConversation() {
        // given
        UUID conversation1 = UUID.randomUUID();
        UUID conversation2 = UUID.randomUUID();

        // Create messages for conversation 1
        createMessageForConversation(conversation1, "First message", ConversationRole.USER);
        createMessageForConversation(conversation1, "Second message", ConversationRole.ASSISTANT);
        createMessageForConversation(conversation1, "Latest message conv1", ConversationRole.USER);

        // Create messages for conversation 2
        createMessageForConversation(conversation2, "Conversation 2 start", ConversationRole.USER);
        createMessageForConversation(conversation2, "Latest message conv2", ConversationRole.ASSISTANT);

        // when
        List<UserConversationResponse> conversations = conversationMessageRepository.findUserConversations(ownerId, projectId);

        // then
        assertThat(conversations).hasSize(2);

        // Conversations should be ordered by latest message timestamp DESC
        assertThat(conversations.get(0).lastMessageAt())
                .isAfterOrEqualTo(conversations.get(1).lastMessageAt());
    }

    @Test
    @DisplayName("findUserConversations filters by project and owner")
    void findUserConversations_filtersByProjectAndOwner() {
        // given
        UUID anotherOwnerId = UUID.randomUUID();
        UUID anotherProjectId = UUID.randomUUID();

        // Create message for current owner and project
        createMessages(2);

        // Create message for another owner
        ConversationMessage otherOwnerMessage = ConversationMessage.builder()
                .conversationId(UUID.randomUUID())
                .content("Other owner message")
                .role(ConversationRole.USER)
                .modelUsed("none")
                .tokensCost(0)
                .ownerId(anotherOwnerId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(0.0)
                .build();
        conversationMessageRepository.save(otherOwnerMessage);

        // Create message for another project
        ConversationMessage otherProjectMessage = ConversationMessage.builder()
                .conversationId(UUID.randomUUID())
                .content("Other project message")
                .role(ConversationRole.USER)
                .modelUsed("none")
                .tokensCost(0)
                .ownerId(ownerId)
                .projectId(anotherProjectId)
                .workspaceId(workspaceId)
                .cost(0.0)
                .build();
        conversationMessageRepository.save(otherProjectMessage);

        // when
        List<UserConversationResponse> conversations = conversationMessageRepository.findUserConversations(ownerId, projectId);

        // then
        assertThat(conversations).hasSize(1);
        assertThat(conversations.get(0).conversationId()).isEqualTo(conversationId);
    }

    private void createMessages(int count) {
        for (int i = 0; i < count; i++) {
            ConversationRole role = (i % 2 == 0) ? ConversationRole.USER : ConversationRole.ASSISTANT;
            createMessageForConversation(conversationId, "Message " + (i + 1), role);
        }
    }

    private void createMessageForConversation(UUID convId, String content, ConversationRole role) {
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(convId)
                .content(content)
                .role(role)
                .modelUsed(role == ConversationRole.ASSISTANT ? "openai/gpt-4" : "none")
                .tokensCost(role == ConversationRole.ASSISTANT ? 100 : 0)
                .ownerId(ownerId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(role == ConversationRole.ASSISTANT ? 0.001 : 0.0)
                .build();
        conversationMessageRepository.save(message);
    }
}
