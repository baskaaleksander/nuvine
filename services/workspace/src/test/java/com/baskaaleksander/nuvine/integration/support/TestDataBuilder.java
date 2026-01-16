package com.baskaaleksander.nuvine.integration.support;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestDataBuilder {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public TestDataBuilder(WorkspaceRepository workspaceRepository,
                           WorkspaceMemberRepository workspaceMemberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    public Workspace createWorkspace(String name, UUID ownerId, String ownerEmail) {
        Workspace workspace = Workspace.builder()
                .name(name)
                .ownerUserId(ownerId)
                .billingTier(BillingTier.FREE)
                .deleted(false)
                .build();
        workspace = workspaceRepository.save(workspace);

        createWorkspaceMember(
                workspace.getId(),
                ownerId,
                ownerEmail,
                "Test Owner",
                WorkspaceRole.OWNER,
                WorkspaceMemberStatus.ACCEPTED
        );

        return workspace;
    }

    public Workspace createWorkspaceOnly(String name, UUID ownerId) {
        Workspace workspace = Workspace.builder()
                .name(name)
                .ownerUserId(ownerId)
                .billingTier(BillingTier.FREE)
                .deleted(false)
                .build();
        return workspaceRepository.save(workspace);
    }

    public WorkspaceMember createWorkspaceMember(UUID workspaceId,
                                                 UUID userId,
                                                 String email,
                                                 String userName,
                                                 WorkspaceRole role,
                                                 WorkspaceMemberStatus status) {
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .email(email)
                .userName(userName)
                .role(role)
                .status(status)
                .deleted(false)
                .build();
        return workspaceMemberRepository.save(member);
    }

    public WorkspaceMember addMemberToWorkspace(UUID workspaceId,
                                                UUID userId,
                                                String email,
                                                WorkspaceRole role) {
        return createWorkspaceMember(
                workspaceId,
                userId,
                email,
                "Test User",
                role,
                WorkspaceMemberStatus.ACCEPTED
        );
    }

    public WorkspaceRepository workspaceRepository() {
        return workspaceRepository;
    }

    public WorkspaceMemberRepository workspaceMemberRepository() {
        return workspaceMemberRepository;
    }

    public void cleanUp() {
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
    }
}
