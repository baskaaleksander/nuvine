package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.infrastructure.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        name = "workspace-service",
        url = "${application.config.workspace-internal-url}",
        configuration = InternalFeignConfig.class
)
public interface WorkspaceServiceServiceClient {
}
