package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.ConversationMessageResponse;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.domain.model.ConversationRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationMessageMapperTest {

    private final ConversationMessageMapper mapper = new ConversationMessageMapper();
    
    @Test
    void toResponse_validMessage_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        ConversationMessage message = ConversationMessage.builder()
                .id(id)
                .conversationId(conversationId)
                .content("Hello, this is a test message")
                .role(ConversationRole.USER)
                .modelUsed("openai/gpt-4")
                .tokensCost(150)
                .ownerId(ownerId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(0.003)
                .createdAt(createdAt)
                .build();

        ConversationMessageResponse response = mapper.toResponse(message);

        assertEquals(id, response.id());
        assertEquals(conversationId, response.conversationId());
        assertEquals("Hello, this is a test message", response.content());
        assertEquals(ConversationRole.USER, response.role());
        assertEquals("openai/gpt-4", response.modelUsed());
        assertEquals(150, response.tokensCost());
        assertEquals(ownerId, response.ownerId());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void toResponse_assistantRole_mapsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        ConversationMessage message = ConversationMessage.builder()
                .id(id)
                .conversationId(conversationId)
                .content("I am an AI assistant response")
                .role(ConversationRole.ASSISTANT)
                .modelUsed("anthropic/claude-3")
                .tokensCost(250)
                .ownerId(ownerId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(0.005)
                .createdAt(createdAt)
                .build();

        ConversationMessageResponse response = mapper.toResponse(message);

        assertEquals(id, response.id());
        assertEquals(conversationId, response.conversationId());
        assertEquals("I am an AI assistant response", response.content());
        assertEquals(ConversationRole.ASSISTANT, response.role());
        assertEquals("anthropic/claude-3", response.modelUsed());
        assertEquals(250, response.tokensCost());
        assertEquals(ownerId, response.ownerId());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void toResponse_zeroTokensCost_mapsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        ConversationMessage message = ConversationMessage.builder()
                .id(id)
                .conversationId(conversationId)
                .content("No context found message")
                .role(ConversationRole.ASSISTANT)
                .modelUsed("openai/gpt-4")
                .tokensCost(0)
                .ownerId(ownerId)
                .projectId(projectId)
                .workspaceId(workspaceId)
                .cost(0)
                .createdAt(createdAt)
                .build();

        ConversationMessageResponse response = mapper.toResponse(message);

        assertEquals(0, response.tokensCost());
    }
}
