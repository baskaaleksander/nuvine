package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("workspaceAccess")
@RequiredArgsConstructor
public class WorkspaceAccessEvaluation {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    public boolean canViewWorkspace(UUID workspaceId, String userId) {
        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId));
    }
}
