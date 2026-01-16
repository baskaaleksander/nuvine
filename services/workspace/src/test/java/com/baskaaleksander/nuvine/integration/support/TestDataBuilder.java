package com.baskaaleksander.nuvine.integration.support;

import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberInviteTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestDataBuilder {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMemberInviteTokenRepository workspaceMemberInviteTokenRepository;
    private final ProjectRepository projectRepository;
    private final DocumentRepository documentRepository;

    public TestDataBuilder(WorkspaceRepository workspaceRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           WorkspaceMemberInviteTokenRepository workspaceMemberInviteTokenRepository,
                           ProjectRepository projectRepository,
                           DocumentRepository documentRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceMemberInviteTokenRepository = workspaceMemberInviteTokenRepository;
        this.projectRepository = projectRepository;
        this.documentRepository = documentRepository;
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

    public Project createProject(UUID workspaceId, String name, String description) {
        Project project = Project.builder()
                .workspaceId(workspaceId)
                .name(name)
                .description(description)
                .deleted(false)
                .build();
        return projectRepository.save(project);
    }

    public Project createProject(UUID workspaceId, String name) {
        return createProject(workspaceId, name, null);
    }

    public Document createDocument(UUID workspaceId, UUID projectId, UUID createdBy, String name, DocumentStatus status) {
        Document document = Document.builder()
                .workspaceId(workspaceId)
                .projectId(projectId)
                .createdBy(createdBy)
                .name(name)
                .status(status)
                .deleted(false)
                .build();
        return documentRepository.save(document);
    }

    public Document createDocument(UUID workspaceId, UUID projectId, UUID createdBy, String name) {
        return createDocument(workspaceId, projectId, createdBy, name, DocumentStatus.UPLOADING);
    }

    public WorkspaceMemberInviteToken createInviteToken(WorkspaceMember member, String token) {
        WorkspaceMemberInviteToken inviteToken = WorkspaceMemberInviteToken.builder()
                .workspaceMember(member)
                .token(token)
                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                .build();
        return workspaceMemberInviteTokenRepository.save(inviteToken);
    }

    public WorkspaceRepository workspaceRepository() {
        return workspaceRepository;
    }

    public WorkspaceMemberRepository workspaceMemberRepository() {
        return workspaceMemberRepository;
    }

    public WorkspaceMemberInviteTokenRepository workspaceMemberInviteTokenRepository() {
        return workspaceMemberInviteTokenRepository;
    }

    public ProjectRepository projectRepository() {
        return projectRepository;
    }

    public DocumentRepository documentRepository() {
        return documentRepository;
    }

    public void cleanUp() {
        workspaceMemberInviteTokenRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        documentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }
}
