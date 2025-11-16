package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberNotFoundException;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("projectAccess")
@RequiredArgsConstructor
public class ProjectAccessEvaluation {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    public boolean canManageProjects(UUID workspaceId, String userId) {
        WorkspaceMember workspaceMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("Workspace not found"));
        return workspaceMember.getRole().equals(WorkspaceRole.OWNER) || workspaceMember.getRole().equals(WorkspaceRole.MODERATOR);
    }

    public boolean canGetProjects(UUID workspaceId, String userId) {
        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId));
    }
}
