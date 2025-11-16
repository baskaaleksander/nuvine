package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("workspaceAccess")
@RequiredArgsConstructor
public class WorkspaceAccessEvaluation {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;

    public boolean canViewWorkspace(UUID workspaceId, String userId) {
        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId));
    }

    public boolean canEditWorkspace(UUID workspaceId, String userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found"));
        return workspace.getOwnerUserId().equals(UUID.fromString(userId));
    }
}
