package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.application.dto.UserConversationResponse;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    @Query("SELECT cm FROM ConversationMessage cm WHERE cm.conversationId = :conversationId ORDER BY cm.createdAt ASC FETCH FIRST :limit ROWS ONLY")
    List<ConversationMessage> findByConversationId(UUID conversationId, int limit);

    Page<ConversationMessage> findAllByConversationId(UUID conversationId, Pageable pageable);

    @Query("""
            SELECT new com.baskaaleksander.nuvine.application.dto.UserConversationResponse(
                cm.conversationId,
                cm.content,
                cm.createdAt
            )
            FROM ConversationMessage cm
            WHERE cm.ownerId = :ownerId
              AND cm.createdAt = (
                    SELECT MAX(cm2.createdAt)
                    FROM ConversationMessage cm2
                    WHERE cm2.conversationId = cm.conversationId
              )
            ORDER BY cm.createdAt DESC
            """)
    List<UserConversationResponse> findUserConversations(UUID ownerId);
}
