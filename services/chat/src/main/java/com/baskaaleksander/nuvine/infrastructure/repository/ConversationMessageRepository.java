package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
}
