package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMapper;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMemberMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.exception.InvalidWorkspaceNameException;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import com.baskaaleksander.nuvine.domain.model.*;
import com.baskaaleksander.nuvine.infrastructure.client.AuthClient;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final AuthClient authClient;

    public WorkspaceCreateResponse createWorkspace(String name, UUID ownerUserId, String ownerEmail) {

        log.info("CREATE_WORKSPACE START userId={}", ownerUserId);

        if (workspaceRepository.existsByNameAndOwnerId(name, ownerUserId)) {
            log.info("CREATE_WORKSPACE FAILED reason=invalid_name userId={}", ownerUserId);
            throw new InvalidWorkspaceNameException("Workspace with name " + name + " already exists");
        }

        Workspace workspace = Workspace.builder()
                .name(name)
                .billingTier(BillingTier.FREE)
                .ownerUserId(ownerUserId)
                .build();

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        UserInternalResponse user;
        try {
            user = authClient.getUserByEmail(ownerEmail);
        } catch (Exception ex) {
            log.info("RESPOND_TO_INVITATION FAILED reason=user_not_found email={}", ownerEmail);
            throw new UserNotFoundException("User not found");
        }

        String ownerName = user.firstName() + " " + user.lastName();

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(savedWorkspace.getId())
                .userId(ownerUserId)
                .email(ownerEmail)
                .userName(ownerName)
                .status(WorkspaceMemberStatus.ACCEPTED)
                .role(WorkspaceRole.OWNER)
                .build();

        workspaceMemberRepository.save(member);

        log.info("CREATE_WORKSPACE SUCCESS userId={} workspaceId={}", ownerUserId, savedWorkspace.getId());

        return workspaceMapper.toWorkspaceCreateResponse(savedWorkspace);
    }

    public PagedResponse<WorkspaceResponse> getWorkspaces(UUID uuid, PaginationRequest request) {
        log.info("GET_WORKSPACES START userId={}", uuid);
        List<UUID> workspaceIds = workspaceMemberRepository.findWorkspaceIdsByUserId(uuid);

        Pageable pageable = PaginationUtil.getPageable(request);
        Page<Workspace> page = workspaceRepository.findAllByIdIn(workspaceIds, pageable);

        List<WorkspaceResponse> content = page.getContent().stream()
                .filter(workspace -> !workspace.isDeleted())
                .map(workspaceMapper::toWorkspaceResponse)
                .toList();

        log.info("GET_WORKSPACES SUCCESS userId={} workspaceCount={}", uuid, content.size());

        return new PagedResponse<>(
                content,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.isLast(),
                page.hasNext()
        );
    }

    public WorkspaceResponseWithStats getWorkspace(UUID workspaceId) {
        log.info("GET_WORKSPACE START workspaceId={}", workspaceId);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.info("GET_WORKSPACES FAILED reason=workspace_not_found workspaceId={}", workspaceId);
                    return new WorkspaceNotFoundException("Workspace not found");
                });

        if (workspace.isDeleted()) {
            log.info("GET_WORKSPACE FAILED reason=workspace_deleted workspaceId={}", workspaceId);
            throw new WorkspaceNotFoundException("Workspace not found");
        }

        Long memberCount = workspaceMemberRepository.getWorkspaceMemberCountByWorkspaceId(workspaceId);
        Long projectCount = projectRepository.getProjectCountByWorkspaceId(workspaceId);

        log.info("GET_WORKSPACE SUCCESS workspaceId={}", workspaceId);

        return new WorkspaceResponseWithStats(
                workspace.getId(),
                workspace.getName(),
                workspace.getOwnerUserId(),
                workspace.getSubscriptionId(),
                workspace.getBillingTier(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt(),
                memberCount,
                projectCount
        );
    }

    @Transactional
    public void updateWorkspace(UUID workspaceId, String name, UUID ownerUserId) {
        log.info("UPDATE_WORKSPACE START workspaceId={}", workspaceId);

        if (workspaceRepository.existsByNameAndOwnerId(name, ownerUserId)) {
            log.info("UPDATE_WORKSPACE FAILED reason=invalid_name userId={}", ownerUserId);
            throw new InvalidWorkspaceNameException("Workspace with name " + name + " already exists");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.info("UPDATE_WORKSPACE FAILED reason=workspace_not_found workspaceId={}", workspaceId);
                    return new WorkspaceNotFoundException("Workspace not found");
                });

        if (workspace.isDeleted()) {
            log.info("UPDATE_WORKSPACE FAILED reason=workspace_deleted workspaceId={}", workspaceId);
            throw new WorkspaceNotFoundException("Workspace not found");
        }

        workspaceRepository.updateWorkspaceName(workspaceId, name);

        log.info("UPDATE_WORKSPACE SUCCESS workspaceId={}", workspaceId);
    }

    public WorkspaceMemberResponse getSelfWorkspaceMember(UUID workspaceId, UUID userId) {
        log.info("GET_SELF_WORKSPACE_MEMBER START workspaceId={} userId={}", workspaceId, userId);
        WorkspaceMember workspaceMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> {
                    log.info("GET_SELF_WORKSPACE_MEMBER FAILED reason=workspace_member_not_found workspaceId={} userId={}", workspaceId, userId);
                    return new WorkspaceMemberNotFoundException("Workspace member not found");
                });

        if (workspaceMember.isDeleted()) {
            log.info("GET_SELF_WORKSPACE_MEMBER FAILED reason=workspace_member_deleted workspaceId={} userId={}", workspaceId, userId);
            throw new WorkspaceMemberNotFoundException("Workspace member not found");
        }

        log.info("GET_SELF_WORKSPACE_MEMBER SUCCESS workspaceId={} userId={}", workspaceId, userId);
        return workspaceMemberMapper.toWorkspaceMemberResponse(workspaceMember);
    }

    @Transactional
    public void deleteWorkspace(UUID workspaceId) {

        if (!workspaceRepository.existsById(workspaceId)) {
            log.info("DELETE_WORKSPACE FAILED reason=workspace_not_found workspaceId={}", workspaceId);
            throw new WorkspaceNotFoundException("Workspace not found");
        }

        workspaceMemberRepository.deleteAllMembersByWorkspaceId(workspaceId);

        workspaceRepository.deleteWorkspace(workspaceId);
    }
}
