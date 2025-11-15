package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.WorkspaceCreateResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceResponse;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceMapper {

    public WorkspaceCreateResponse toWorkspaceCreateResponse(Workspace workspace) {
        return new WorkspaceCreateResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getOwnerUserId(),
                workspace.getBillingTier(),
                workspace.getCreatedAt()
        );
    }

    public WorkspaceResponse toWorkspaceResponse(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getOwnerUserId(),
                workspace.getSubscriptionId() != null ? workspace.getSubscriptionId() : null,
                workspace.getBillingTier(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }
}
