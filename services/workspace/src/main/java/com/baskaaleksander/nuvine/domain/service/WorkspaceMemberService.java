package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMembersResponse;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMemberMapper;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
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

        if (!workspaceRepository.existsById(workspaceId)) {
            throw new WorkspaceNotFoundException("Workspace not found");
        }
        
        Long count = workspaceMemberRepository.getWorkspaceMemberCountByWorkspaceId(workspaceId);

        List<WorkspaceMemberResponse> members = workspaceMemberRepository.getWorkspaceMembersByWorkspaceId(workspaceId).stream()
                .map(workspaceMemberMapper::toWorkspaceMemberResponse)
                .toList();

        return new WorkspaceMembersResponse(members, count);
    }
}
