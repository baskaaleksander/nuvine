package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.application.dto.UploadCompletedRequest;
import com.baskaaleksander.nuvine.infrastructure.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "workspace-service",
        url = "${application.config.api-base-url}",
        contextId = "workspaceServiceServiceClient",
        configuration = InternalFeignConfig.class
)
public interface WorkspaceServiceServiceClient {

    @PatchMapping("/internal/documents/{documentId}/upload-completed")
    void uploadCompleted(@PathVariable UUID documentId, @RequestBody UploadCompletedRequest request);
}
