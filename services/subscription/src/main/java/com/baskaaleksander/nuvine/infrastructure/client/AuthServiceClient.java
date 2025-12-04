package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.infrastructure.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "auth-service",
        url = "${application.config.api-base-url}",
        contextId = "authServiceClient",
        configuration = InternalFeignConfig.class
)
public interface AuthServiceClient {

    @GetMapping("/internal/auth/users/{userId}")
    UserInternalResponse getUserInternalResponse(@PathVariable UUID userId);
}
