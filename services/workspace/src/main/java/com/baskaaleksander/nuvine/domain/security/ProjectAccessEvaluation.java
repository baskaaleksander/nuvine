package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.domain.exception.ProjectNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Project;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("projectAccess")
@RequiredArgsConstructor
public class ProjectAccessEvaluation {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectRepository projectRepository;

    public boolean canManageProjectsInWorkspace(UUID workspaceId, String userId) {
        WorkspaceMember workspaceMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("Workspace not found"));
        return workspaceMember.getRole().equals(WorkspaceRole.OWNER) || workspaceMember.getRole().equals(WorkspaceRole.MODERATOR);
    }

    public boolean canGetProjectsInWorkspace(UUID workspaceId, String userId) {
        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId));
    }

    public boolean canManageProject(UUID projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(project.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("Workspace not found"));

        return member.getRole().equals(WorkspaceRole.OWNER) || member.getRole().equals(WorkspaceRole.MODERATOR);
    }

    public boolean canGetProject(UUID projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(project.getWorkspaceId(), UUID.fromString(userId));
    }
}
