package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceCacheWrapper {

    private final AuthServiceClient authServiceClient;

    @Cacheable(cacheNames = "users", key = "#userId")
    public UserInternalResponse getUserInternalResponse(UUID userId) {
        return authServiceClient.getUserInternalResponse(userId);
    }
}
