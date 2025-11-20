package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.infrastructure.config.UserFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "workspace-service",
        url = "${application.config.workspace-internal-url}",
        configuration = UserFeignConfig.class
)
public interface WorkspaceServiceUserClient {
}
