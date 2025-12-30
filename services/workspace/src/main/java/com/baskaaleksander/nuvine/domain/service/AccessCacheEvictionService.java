package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessCacheEvictionService {

    private final CacheManager cacheManager;
    private final ProjectRepository projectRepository;

    public void evictAccessForUserInWorkspace(UUID workspaceId, UUID userId) {
        log.debug("CACHE_EVICT_ACCESS START workspaceId={} userId={}", workspaceId, userId);

        String wsKey = workspaceId.toString() + ":" + userId.toString();

        evict("access-workspace-view", wsKey);
        evict("access-workspace-edit", wsKey);
        evict("access-project-manage", wsKey);

        List<UUID> projectIds = projectRepository.findProjectIdsByWorkspaceId(workspaceId);
        for (UUID projectId : projectIds) {
            String projKey = projectId.toString() + ":" + userId.toString();
            evict("access-project-manage", projKey);
            evict("access-project-view", projKey);
        }

        log.debug("CACHE_EVICT_ACCESS END workspaceId={} userId={} projectsEvicted={}", workspaceId, userId, projectIds.size());
    }

    private void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
