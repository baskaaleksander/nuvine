package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAccessEvaluationTest {

    @Mock
    private ConversationMessageRepository conversationMessageRepository;

    @InjectMocks
    private ChatAccessEvaluation chatAccessEvaluation;

    private UUID chatId;
    private UUID ownerId;
    private UUID projectId;
    private UUID workspaceId;
    private String ownerIdString;
    private ConversationMessage message;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        ownerIdString = ownerId.toString();

        message = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(chatId)
                .content("Test message")
                .role(ConversationRole.USER)
                .modelUsed("openai/gpt-4")
                .tokensCost(100)
                .ownerId(ownerId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(0.01)
                .createdAt(Instant.now())
                .build();
    }
    
    @Test
    void canAccessChat_owner_returnsTrue() {
        when(conversationMessageRepository.findByConversationId(chatId, 1))
                .thenReturn(List.of(message));

        boolean result = chatAccessEvaluation.canAccessChat(chatId, ownerIdString);

        assertTrue(result);
        verify(conversationMessageRepository).findByConversationId(chatId, 1);
    }

    @Test
    void canAccessChat_nonOwner_returnsFalse() {
        UUID differentUserId = UUID.randomUUID();
        when(conversationMessageRepository.findByConversationId(chatId, 1))
                .thenReturn(List.of(message));

        boolean result = chatAccessEvaluation.canAccessChat(chatId, differentUserId.toString());

        assertFalse(result);
        verify(conversationMessageRepository).findByConversationId(chatId, 1);
    }

    @Test
    void canCreateMessage_nullChatId_returnsTrue() {
        boolean result = chatAccessEvaluation.canCreateMessage(null, ownerIdString, projectId, workspaceId);

        assertTrue(result);
    }

    @Test
    void canCreateMessage_emptyMessages_returnsTrue() {
        when(conversationMessageRepository.findByConversationId(chatId, 1))
                .thenReturn(List.of());

        boolean result = chatAccessEvaluation.canCreateMessage(chatId, ownerIdString, projectId, workspaceId);

        assertTrue(result);
        verify(conversationMessageRepository).findByConversationId(chatId, 1);
    }

    @Test
    void canCreateMessage_matchingOwnerAndIds_returnsTrue() {
        when(conversationMessageRepository.findByConversationId(chatId, 1))
                .thenReturn(List.of(message));

        boolean result = chatAccessEvaluation.canCreateMessage(chatId, ownerIdString, projectId, workspaceId);

        assertTrue(result);
        verify(conversationMessageRepository).findByConversationId(chatId, 1);
    }

    @Test
    void canCreateMessage_mismatchedWorkspace_returnsFalse() {
        UUID differentWorkspaceId = UUID.randomUUID();
        when(conversationMessageRepository.findByConversationId(chatId, 1))
                .thenReturn(List.of(message));

        boolean result = chatAccessEvaluation.canCreateMessage(chatId, ownerIdString, projectId, differentWorkspaceId);

        assertFalse(result);
        verify(conversationMessageRepository).findByConversationId(chatId, 1);
    }

    @Test
    void canCreateMessage_mismatchedProject_returnsFalse() {
        UUID differentProjectId = UUID.randomUUID();
        when(conversationMessageRepository.findByConversationId(chatId, 1))
                .thenReturn(List.of(message));

        boolean result = chatAccessEvaluation.canCreateMessage(chatId, ownerIdString, differentProjectId, workspaceId);

        assertFalse(result);
        verify(conversationMessageRepository).findByConversationId(chatId, 1);
    }

    @Test
    void canCreateMessage_nonOwner_returnsFalse() {
        UUID differentUserId = UUID.randomUUID();
        when(conversationMessageRepository.findByConversationId(chatId, 1))
                .thenReturn(List.of(message));

        boolean result = chatAccessEvaluation.canCreateMessage(chatId, differentUserId.toString(), projectId, workspaceId);

        assertFalse(result);
        verify(conversationMessageRepository).findByConversationId(chatId, 1);
    }
}
