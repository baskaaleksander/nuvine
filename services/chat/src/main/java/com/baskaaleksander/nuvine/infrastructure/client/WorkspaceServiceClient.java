package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.infrastructure.config.UserFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "workspace-service",
        url = "${application.config.api-base-url}",
        contextId = "workspaceServiceClient",
        configuration = UserFeignConfig.class
)
public interface WorkspaceServiceClient {

    @GetMapping("/workspaces/{workspaceId}")
    void checkWorkspaceAccess(@PathVariable UUID workspaceId);

    @GetMapping("/internal/projects/{projectId}/document-ids")
    List<UUID> getDocumentIdsInProject(@PathVariable UUID projectId);

}
