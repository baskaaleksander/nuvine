package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.infrastructure.interceptor.OAuth2ClientCredentialsInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "auth-service",
        url = "${application.config.auth-internal-url}",
        configuration = OAuth2ClientCredentialsInterceptor.class
)
public interface AuthClient {

    @GetMapping("/users/{userId}")
    UserInternalResponse checkInternalUser(@PathVariable UUID userId);

    @GetMapping("/users/email")
    UserInternalResponse getUserByEmail(@RequestParam String email);
}
