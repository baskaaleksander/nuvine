package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("chatAccess")
@RequiredArgsConstructor
public class ChatAccessEvaluation {

    private final ConversationMessageRepository conversationMessageRepository;

    public boolean canAccessChat(UUID chatId, String userId) {

        var message = conversationMessageRepository.findByConversationId(chatId, 1);

        if (message.isEmpty()) {
            return false;
        }

        return message.getFirst().getOwnerId().equals(UUID.fromString(userId));
    }

    public boolean canCreateMessage(UUID chatId, String userId, UUID projectId, UUID workspaceId) {
        if (chatId == null) {
            return true;
        }

        var message = conversationMessageRepository.findByConversationId(chatId, 1);

        if (message.isEmpty()) return true;

        if (!message.getFirst().getProjectId().equals(projectId) || !message.getFirst().getWorkspaceId().equals(workspaceId))
            return false;

        return message.getFirst().getOwnerId().equals(UUID.fromString(userId));
    }
}
