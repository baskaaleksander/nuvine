package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMembersResponse;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMemberMapper;
import com.baskaaleksander.nuvine.domain.exception.*;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.client.AuthClient;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberAddedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberInvitedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceMemberAddedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceMemberInvitedEventProducer;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberInviteToken;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    private final AuthClient authClient;
    private final WorkspaceMemberAddedEventProducer workspaceMemberAddedEventProducer;
    private final WorkspaceMemberInvitedEventProducer workspaceMemberInvitedEventProducer;
    private final WorkspaceMemberInviteTokenService workspaceMemberInviteTokenService;
    private final AccessCacheEvictionService accessCacheEvictionService;
    private final EntityCacheEvictionService entityCacheEvictionService;

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
        UserInternalResponse user;
        try {
            user = authClient.checkInternalUser(userId);
        } catch (Exception ex) {
            log.info("ADD_WORKSPACE_MEMBER FAILED reason=user_not_found workspaceId={}, userId={}", workspaceId, userId);
            throw new UserNotFoundException("User not found");
        }

        if (role == WorkspaceRole.OWNER) {
            log.info("ADD_WORKSPACE_MEMBER FAILED reason=owner_not_allowed workspaceId={}, userId={}", workspaceId, userId);
            throw new WorkspaceRoleConflictException("Owner role is not allowed");
        }
        
        WorkspaceMember existing = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElse(null);

        if (existing != null) {
            if (existing.isDeleted()) {
                workspaceMemberRepository.updateDeletedById(existing.getId(), false);
                workspaceMemberRepository.updateMemberRole(userId, workspaceId, role);

                accessCacheEvictionService.evictAccessForUserInWorkspace(workspaceId, userId);
                entityCacheEvictionService.evictWorkspaceMember(workspaceId, userId);

                log.info("ADD_WORKSPACE_MEMBER REACTIVATED workspaceId={}, userId={}, role={}", workspaceId, userId, role);
                return;
            }

            log.info("ADD_WORKSPACE_MEMBER FAILED reason=member_exists workspaceId={}, userId={}", workspaceId, userId);
            throw new WorkspaceMemberExistsException("Member already exists");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .role(role)
                .build();

        workspaceMemberRepository.save(member);


        workspaceMemberAddedEventProducer.sendWorkspaceMemberAddedEvent(
                new WorkspaceMemberAddedEvent(
                        user.email(),
                        userId.toString(),
                        workspaceId.toString(),
                        role.toString()
                )
        );

        accessCacheEvictionService.evictAccessForUserInWorkspace(workspaceId, userId);
        entityCacheEvictionService.evictWorkspaceMember(workspaceId, userId);

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
                throw new WorkspaceRoleConflictException("User is already the owner");
            }

            workspaceMemberRepository.updateMemberRole(userId, workspaceId, WorkspaceRole.OWNER);
            workspaceMemberRepository.updateMemberRole(ownerId, workspaceId, WorkspaceRole.MODERATOR);

            accessCacheEvictionService.evictAccessForUserInWorkspace(workspaceId, userId);
            accessCacheEvictionService.evictAccessForUserInWorkspace(workspaceId, ownerId);
            entityCacheEvictionService.evictWorkspaceMember(workspaceId, userId);
            entityCacheEvictionService.evictWorkspaceMember(workspaceId, ownerId);
        } else {
            if (member.getRole() == WorkspaceRole.OWNER) {
                log.info("UPDATE_WORKSPACE_MEMBER_ROLE FAILED reason=owner_cannot_be_downgraded workspaceId={}, userId={}", workspaceId, userId);
                throw new WorkspaceRoleConflictException("Owner cannot be downgraded");
            }
            if (member.getRole().equals(role)) {
                log.info("UPDATE_WORKSPACE_MEMBER_ROLE FAILED reason=role_is_already_assigned workspaceId={} userId={}", workspaceId, userId);
                throw new WorkspaceRoleConflictException("Cannot assign same role");
            }
            workspaceMemberRepository.updateMemberRole(userId, workspaceId, role);

            accessCacheEvictionService.evictAccessForUserInWorkspace(workspaceId, userId);
            entityCacheEvictionService.evictWorkspaceMember(workspaceId, userId);
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
            throw new WorkspaceOwnerRemovalException("Owner cannot be removed");
        }

        workspaceMemberRepository.updateDeletedById(existing.getId(), true);

        accessCacheEvictionService.evictAccessForUserInWorkspace(workspaceId, userId);
        entityCacheEvictionService.evictWorkspaceMember(workspaceId, userId);

        log.info("REMOVE_WORKSPACE_MEMBER END workspaceId={}, userId={}", workspaceId, userId);
    }

    @Cacheable(value = "entity-workspace-member", key = "#workspaceId.toString() + ':' + #uuid.toString()")
    public WorkspaceMemberResponse getWorkspaceMember(UUID workspaceId, UUID uuid) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, uuid)
                .map(workspaceMemberMapper::toWorkspaceMemberResponse)
                .orElseThrow(() -> new WorkspaceMemberNotFoundException("Workspace member not found"));
    }

    @Transactional
    public void inviteWorkspaceMember(UUID workspaceId, String email, WorkspaceRole role) {
        log.info("INVITE_WORKSPACE_MEMBER START workspaceId={}, role={}", workspaceId, role);

        if (role == WorkspaceRole.OWNER) {
            log.info("INVITE_WORKSPACE_MEMBER FAILED reason=owner_not_allowed workspaceId={}", workspaceId);
            throw new WorkspaceRoleConflictException("Owner role is not allowed");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> {
                    log.info("INVITE_WORKSPACE_MEMBER FAILED reason=workspace_not_found workspaceId={}", workspaceId);
                    return new WorkspaceNotFoundException("Workspace not found");
                });

        WorkspaceMember existing = workspaceMemberRepository
                .findByWorkspaceIdAndEmail(workspaceId, email)
                .orElse(null);

        if (existing != null && !existing.isDeleted() && existing.getStatus() == WorkspaceMemberStatus.ACCEPTED) {
            log.info("INVITE_WORKSPACE_MEMBER FAILED reason=member_exists workspaceId={}", workspaceId);
            throw new WorkspaceMemberExistsException("Member already exists");
        }

        boolean forceNewToken = false;
        WorkspaceMember member;

        if (existing != null) {
            member = existing;

            if (member.isDeleted() || member.getStatus() == WorkspaceMemberStatus.REJECTED) {
                forceNewToken = true;
            }

            member.setDeleted(false);
            member.setStatus(WorkspaceMemberStatus.PENDING);
            member.setRole(role);
            member.setEmail(email);
            member = workspaceMemberRepository.save(member);
        } else {
            forceNewToken = true;
            member = WorkspaceMember.builder()
                    .workspaceId(workspaceId)
                    .email(email)
                    .role(role)
                    .status(WorkspaceMemberStatus.PENDING)
                    .build();

            member = workspaceMemberRepository.save(member);
        }

        WorkspaceMemberInviteToken inviteToken = workspaceMemberInviteTokenService.getOrCreateActiveToken(member, forceNewToken);

        workspaceMemberInvitedEventProducer.sendWorkspaceMemberInvitedEvent(
                new WorkspaceMemberInvitedEvent(
                        email,
                        workspaceId.toString(),
                        workspace.getName(),
                        role.toString(),
                        inviteToken.getToken()
                )
        );

        log.info("INVITE_WORKSPACE_MEMBER END workspaceId={}", workspaceId);
    }
}
