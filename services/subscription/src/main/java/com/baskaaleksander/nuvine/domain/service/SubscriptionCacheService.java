package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.Subscription;
import com.baskaaleksander.nuvine.infrastructure.persistence.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionCacheService {

    private final SubscriptionRepository subscriptionRepository;

    @Cacheable(cacheNames = "subscriptions", key = "#workspaceId")
    public Optional<Subscription> findByWorkspaceId(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId);
    }

    @CacheEvict(cacheNames = "subscriptions", key = "#workspaceId")
    public void evictSubscriptionCache(UUID workspaceId) {
        log.debug("Evicted subscription cache for workspaceId={}", workspaceId);
    }
}
