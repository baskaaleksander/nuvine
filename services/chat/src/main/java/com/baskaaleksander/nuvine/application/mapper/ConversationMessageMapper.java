package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.ConversationMessageResponse;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import org.springframework.stereotype.Component;

@Component
public class ConversationMessageMapper {

    public ConversationMessageResponse toResponse(ConversationMessage message) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getContent(),
                message.getRole(),
                message.getModelUsed(),
                message.getTokensCost(),
                message.getOwnerId(),
                message.getCreatedAt()
        );
    }
}
