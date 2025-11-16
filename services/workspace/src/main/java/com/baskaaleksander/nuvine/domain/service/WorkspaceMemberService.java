package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMembersResponse;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMemberMapper;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public void addWorkspaceMember(UUID workspaceId, UUID uuid, WorkspaceRole role) {
        log.info("ADD_WORKSPACE_MEMBER START workspaceId={}, uuid={}, role={}", workspaceId, uuid, role);

        if (role == WorkspaceRole.OWNER) {
            log.info("ADD_WORKSPACE_MEMBER FAILED reason=owner_not_allowed workspaceId={}", workspaceId);
            throw new IllegalArgumentException("Owner role is not allowed");
        }

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, uuid)) {
            log.info("ADD_WORKSPACE_MEMBER FAILED reason=member_exists workspaceId={}", workspaceId);
            throw new IllegalArgumentException("Member already exists");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(uuid)
                .role(role)
                .build();

        workspaceMemberRepository.save(member);

        log.info("ADD_WORKSPACE_MEMBER END workspaceId={}", workspaceId);
    }
}
