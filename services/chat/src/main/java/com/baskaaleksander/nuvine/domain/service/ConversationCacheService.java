package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.ConversationMessageResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
import com.baskaaleksander.nuvine.application.dto.UserConversationResponse;
import com.baskaaleksander.nuvine.application.mapper.ConversationMessageMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.model.ConversationMessage;
import com.baskaaleksander.nuvine.infrastructure.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.baskaaleksander.nuvine.infrastructure.config.CacheConfiguration.CONVERSATION_MESSAGES_CACHE;
import static com.baskaaleksander.nuvine.infrastructure.config.CacheConfiguration.USER_CONVERSATIONS_CACHE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationCacheService {

    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationMessageMapper mapper;

    @Cacheable(
            cacheNames = USER_CONVERSATIONS_CACHE,
            key = "#ownerId.toString() + ':' + #projectId.toString()"
    )
    public List<UserConversationResponse> findUserConversations(UUID ownerId, UUID projectId) {
        log.debug("Cache MISS for user conversations: ownerId={}, projectId={}", ownerId, projectId);
        return conversationMessageRepository.findUserConversations(ownerId, projectId);
    }

    @Cacheable(
            cacheNames = CONVERSATION_MESSAGES_CACHE,
            key = "#conversationId.toString() + ':' + #request.getPage() + ':' + #request.getSize() + ':' + #request.getSortField() + ':' + #request.getDirection()"
    )
    public PagedResponse<ConversationMessageResponse> findMessages(
            UUID conversationId,
            PaginationRequest request
    ) {
        log.debug("Cache MISS for conversation messages: conversationId={}, page={}",
                conversationId, request.getPage());

        Pageable pageable = PaginationUtil.getPageable(request);
        Page<ConversationMessage> page =
                conversationMessageRepository.findAllByConversationId(conversationId, pageable);

        List<ConversationMessageResponse> content = page.getContent().stream()
                .map(mapper::toResponse)
                .toList();

        return new PagedResponse<>(
                content,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.isLast(),
                page.hasNext()
        );
    }

    @CacheEvict(
            cacheNames = USER_CONVERSATIONS_CACHE,
            key = "#ownerId.toString() + ':' + #projectId.toString()"
    )
    public void evictUserConversationsCache(UUID ownerId, UUID projectId) {
        log.debug("Evicted user conversations cache: ownerId={}, projectId={}", ownerId, projectId);
    }

    @CacheEvict(
            cacheNames = CONVERSATION_MESSAGES_CACHE,
            allEntries = true
    )
    public void evictConversationMessagesCache(UUID conversationId) {
        log.debug("Evicted conversation messages cache for conversationId={}", conversationId);
    }

    @Caching(evict = {
            @CacheEvict(
                    cacheNames = USER_CONVERSATIONS_CACHE,
                    key = "#ownerId.toString() + ':' + #projectId.toString()"
            ),
            @CacheEvict(
                    cacheNames = CONVERSATION_MESSAGES_CACHE,
                    allEntries = true
            )
    })
    public void evictAfterNewMessage(UUID ownerId, UUID projectId, UUID conversationId) {
        log.debug("Evicted caches after new message: ownerId={}, projectId={}, conversationId={}",
                ownerId, projectId, conversationId);
    }
}
