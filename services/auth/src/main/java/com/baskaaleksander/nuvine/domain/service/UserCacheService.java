package com.baskaaleksander.nuvine.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final CacheManager cacheManager;

    public void evictUserById(UUID userId) {
        String key = userId.toString();

        evict("users", key);
        evict("users-internal", key);
        evict("users-admin", key);
    }

    public void evictUserInternalByEmail(String email) {
        evict("users-internal", email);
    }

    private void evict(String cacheName, String key) {
        Cache<String, Object> cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.remove(key);
        }
    }
}