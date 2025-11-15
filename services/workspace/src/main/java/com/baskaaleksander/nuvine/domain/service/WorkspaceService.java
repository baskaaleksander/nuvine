package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.WorkspaceCreateResponse;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMapper;
import com.baskaaleksander.nuvine.domain.exception.InvalidWorkspaceNameException;
import com.baskaaleksander.nuvine.domain.model.BillingTier;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMapper workspaceMapper;

    public WorkspaceCreateResponse createWorkspace(String name, UUID ownerUserId) {

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

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(savedWorkspace.getId())
                .userId(ownerUserId)
                .role(WorkspaceRole.OWNER)
                .build();

        workspaceMemberRepository.save(member);

        log.info("CREATE_WORKSPACE SUCCESS userId={} workspaceId={}", ownerUserId, savedWorkspace.getId());
        
        return workspaceMapper.toWorkspaceCreateResponse(savedWorkspace);
    }
}
