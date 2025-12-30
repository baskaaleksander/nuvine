package com.baskaaleksander.nuvine.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.baskaaleksander.nuvine.infrastructure.config.CacheConfiguration.INGESTION_JOB_CACHE;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionJobCacheService {

    private final CacheManager cacheManager;

    public void evictByDocumentId(UUID documentId) {
        evict(INGESTION_JOB_CACHE, documentId.toString());
        log.debug("Evicted {} cache for documentId={}", INGESTION_JOB_CACHE, documentId);
    }

    private void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
