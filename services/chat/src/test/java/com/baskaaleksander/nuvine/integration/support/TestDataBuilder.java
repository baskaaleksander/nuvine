package com.baskaaleksander.nuvine.integration.support;

import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TestDataBuilder {

    private final ConversationMessageRepository conversationMessageRepository;

    public TestDataBuilder(ConversationMessageRepository conversationMessageRepository) {
        this.conversationMessageRepository = conversationMessageRepository;
    }

    public ConversationMessage createUserMessage(
            UUID conversationId,
            UUID ownerId,
            UUID projectId,
            UUID workspaceId,
            String content
    ) {
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(conversationId)
                .content(content)
                .role(ConversationRole.USER)
                .modelUsed("none")
                .tokensCost(0)
                .ownerId(ownerId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(0.0)
                .build();
        return conversationMessageRepository.save(message);
    }

    public ConversationMessage createAssistantMessage(
            UUID conversationId,
            UUID ownerId,
            UUID projectId,
            UUID workspaceId,
            String content,
            String model,
            int tokensCost,
            double cost
    ) {
        ConversationMessage message = ConversationMessage.builder()
                .conversationId(conversationId)
                .content(content)
                .role(ConversationRole.ASSISTANT)
                .modelUsed(model)
                .tokensCost(tokensCost)
                .ownerId(ownerId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(cost)
                .build();
        return conversationMessageRepository.save(message);
    }

    public List<ConversationMessage> createConversationWithMessages(
            UUID ownerId,
            UUID projectId,
            UUID workspaceId,
            int messageCount
    ) {
        UUID conversationId = UUID.randomUUID();
        List<ConversationMessage> messages = new ArrayList<>();

        for (int i = 0; i < messageCount; i++) {
            if (i % 2 == 0) {
                messages.add(createUserMessage(
                        conversationId,
                        ownerId,
                        projectId,
                        workspaceId,
                        "User message " + (i + 1)
                ));
            } else {
                messages.add(createAssistantMessage(
                        conversationId,
                        ownerId,
                        projectId,
                        workspaceId,
                        "Assistant response " + (i + 1),
                        "openai/gpt-4",
                        100 + i * 10,
                        0.001 * (i + 1)
                ));
            }
        }

        return messages;
    }

    public void cleanUp() {
        conversationMessageRepository.deleteAll();
    }
}
