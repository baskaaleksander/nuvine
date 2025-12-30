package com.baskaaleksander.nuvine.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityCacheEvictionService {

    private final CacheManager cacheManager;

    public void evictWorkspace(UUID workspaceId) {
        log.debug("CACHE_EVICT_ENTITY workspace workspaceId={}", workspaceId);
        evict("entity-workspace", workspaceId.toString());
        evict("entity-workspace-subscription", workspaceId.toString());
    }

    public void evictWorkspaceMember(UUID workspaceId, UUID userId) {
        log.debug("CACHE_EVICT_ENTITY workspace-member workspaceId={} userId={}", workspaceId, userId);
        String key = workspaceId.toString() + ":" + userId.toString();
        evict("entity-workspace-member", key);
    }

    public void evictProject(UUID projectId) {
        log.debug("CACHE_EVICT_ENTITY project projectId={}", projectId);
        evict("entity-project", projectId.toString());
    }

    public void evictDocument(UUID documentId) {
        log.debug("CACHE_EVICT_ENTITY document documentId={}", documentId);
        evict("entity-document", documentId.toString());
        evict("entity-document-internal", documentId.toString());
    }

    private void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
