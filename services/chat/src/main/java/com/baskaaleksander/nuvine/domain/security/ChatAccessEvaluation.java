package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("chatAccess")
@RequiredArgsConstructor
public class ChatAccessEvaluation {

    private final ConversationMessageRepository conversationMessageRepository;

    public boolean canCreateMessage(UUID chatId, String userId) {
        if (chatId == null) {
            return true;
        }
        var message = conversationMessageRepository.findByConversationId(chatId, 1);

        if (message.isEmpty()) return true;

        return message.getFirst().getOwnerId().equals(UUID.fromString(userId));
    }
}
