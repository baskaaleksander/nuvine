package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMembersResponse;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMemberMapper;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceMembersResponse getWorkspaceMembers(UUID workspaceId) {

        log.info("GET_WORKSPACE_MEMBERS START workspaceId={}", workspaceId);
        if (!workspaceRepository.existsById(workspaceId)) {
            log.info("GET_WORKSPACE_MEMBERS FAILED reason=workspace_not_found workspaceId={}", workspaceId);
            throw new WorkspaceNotFoundException("Workspace not found");
        }

        Long count = workspaceMemberRepository.getWorkspaceMemberCountByWorkspaceId(workspaceId);

        List<WorkspaceMemberResponse> members = workspaceMemberRepository.getWorkspaceMembersByWorkspaceId(workspaceId).stream()
                .map(workspaceMemberMapper::toWorkspaceMemberResponse)
                .toList();

        log.info("GET_WORKSPACE_MEMBERS END workspaceId={}", workspaceId);
        return new WorkspaceMembersResponse(members, count);
    }

    @Transactional
    public void addWorkspaceMember(UUID workspaceId, UUID userId, WorkspaceRole role) {
        log.info("ADD_WORKSPACE_MEMBER START workspaceId={}, userId={}, role={}", workspaceId, userId, role);

        //call to auth service here

        if (role == WorkspaceRole.OWNER) {
            log.info("ADD_WORKSPACE_MEMBER FAILED reason=owner_not_allowed workspaceId={}, userId={}", workspaceId, userId);
            throw new IllegalArgumentException("Owner role is not allowed");
        }

        WorkspaceMember existing = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElse(null);

        if (existing != null) {
            if (existing.isDeleted()) {
                workspaceMemberRepository.updateDeletedById(existing.getId(), false);
                workspaceMemberRepository.updateMemberRole(userId, workspaceId, role);

                log.info("ADD_WORKSPACE_MEMBER REACTIVATED workspaceId={}, userId={}, role={}", workspaceId, userId, role);
                return;
            }

            log.info("ADD_WORKSPACE_MEMBER FAILED reason=member_exists workspaceId={}, userId={}", workspaceId, userId);
            throw new IllegalArgumentException("Member already exists");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .role(role)
                .build();

        workspaceMemberRepository.save(member);

        log.info("ADD_WORKSPACE_MEMBER END workspaceId={}, userId={}", workspaceId, userId);
    }

    @Transactional
    public void updateWorkspaceMemberRole(UUID workspaceId, UUID userId, WorkspaceRole role) {
        log.info("UPDATE_WORKSPACE_MEMBER_ROLE START workspaceId={}, userId={}, role={}", workspaceId, userId, role);

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> {
                    log.info("UPDATE_WORKSPACE_MEMBER_ROLE FAILED reason=workspace_member_not_found workspaceId={}, userId={}", workspaceId, userId);
                    return new WorkspaceMemberNotFoundException("Workspace member not found");
                });

        if (member.isDeleted()) {
            log.info("UPDATE_WORKSPACE_MEMBER_ROLE FAILED reason=workspace_member_deleted workspaceId={}, userId={}", workspaceId, userId);
            throw new WorkspaceMemberNotFoundException("Workspace member not found");
        }

        if (role == WorkspaceRole.OWNER) {
            UUID ownerId = workspaceMemberRepository.findOwnerUserIdByWorkspaceId(workspaceId)
                    .orElseThrow(() -> {
                        log.info("UPDATE_WORKSPACE_MEMBER_ROLE FAILED reason=workspace_owner_not_found workspaceId={}", workspaceId);
                        return new WorkspaceMemberNotFoundException("Workspace owner not found");
                    });

            if (ownerId.equals(userId)) {
                log.info("UPDATE_WORKSPACE_MEMBER_ROLE FAILED reason=user_already_owner workspaceId={}, userId={}", workspaceId, userId);
                throw new IllegalArgumentException("User is already the owner");
            }

            workspaceMemberRepository.updateMemberRole(userId, workspaceId, WorkspaceRole.OWNER);
            workspaceMemberRepository.updateMemberRole(ownerId, workspaceId, WorkspaceRole.MODERATOR);
        } else {
            if (member.getRole() == WorkspaceRole.OWNER) {
                log.info("UPDATE_WORKSPACE_MEMBER_ROLE FAILED reason=owner_cannot_be_downgraded workspaceId={}, userId={}", workspaceId, userId);
                throw new IllegalArgumentException("Owner cannot be downgraded");
            }
            if (member.getRole().equals(role)) {
                log.info("UPDATE_WORKSPACE_MEMBER_ROLE FAILED reason=role_is_already_assigned workspaceId={} userId={}", workspaceId, userId);
                throw new IllegalArgumentException("Cannot assign same role");
            }
            workspaceMemberRepository.updateMemberRole(userId, workspaceId, role);
        }

        log.info("UPDATE_WORKSPACE_MEMBER_ROLE END workspaceId={}", workspaceId);
    }

    @Transactional
    public void removeWorkspaceMember(UUID workspaceId, UUID userId) {
        log.info("REMOVE_WORKSPACE_MEMBER START workspaceId={}, userId={}", workspaceId, userId);

        WorkspaceMember existing = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> {
                    log.info("REMOVE_WORKSPACE_MEMBER FAILED reason=workspace_member_not_found workspaceId={}, userId={}", workspaceId, userId);
                    return new WorkspaceMemberNotFoundException("Workspace member not found");
                });

        if (existing.isDeleted()) {
            log.info("REMOVE_WORKSPACE_MEMBER FAILED reason=workspace_member_deleted workspaceId={}, userId={}", workspaceId, userId);
            throw new WorkspaceMemberNotFoundException("Workspace member not found");
        }

        if (existing.getRole() == WorkspaceRole.OWNER) {
            log.info("REMOVE_WORKSPACE_MEMBER FAILED reason=owner_cannot_be_removed workspaceId={}, userId={}", workspaceId, userId);
            throw new IllegalArgumentException("Owner cannot be removed");
        }

        workspaceMemberRepository.updateDeletedById(existing.getId(), true);

        log.info("REMOVE_WORKSPACE_MEMBER END workspaceId={}, userId={}", workspaceId, userId);
    }
}
