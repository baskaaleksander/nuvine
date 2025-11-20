package com.baskaaleksander.nuvine.infrastructure.interceptor;

import com.baskaaleksander.nuvine.infrastructure.auth.KeycloakClientCredentialsTokenProvider;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2ClientCredentialsInterceptor implements RequestInterceptor {

    private final KeycloakClientCredentialsTokenProvider tokenProvider;

    @Override
    public void apply(RequestTemplate template) {
        String token = tokenProvider.getAccessToken();
        template.header("Authorization", "Bearer " + token);
    }
}
