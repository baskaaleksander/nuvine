package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.WorkspaceCreateResponse;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMapper;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMemberMapper;
import com.baskaaleksander.nuvine.domain.exception.InvalidWorkspaceNameException;
import com.baskaaleksander.nuvine.domain.model.BillingTier;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private WorkspaceMapper workspaceMapper;
    @Mock
    private WorkspaceMemberMapper workspaceMemberMapper;

    @InjectMocks
    private WorkspaceService workspaceService;

    private UUID ownerUserId;
    private String workspaceName;
    private Workspace savedWorkspace;
    private WorkspaceCreateResponse workspaceCreateResponse;

    @BeforeEach
    void setUp() {
        ownerUserId = UUID.randomUUID();
        workspaceName = "Test Workspace";
        savedWorkspace = Workspace.builder()
                .id(UUID.randomUUID())
                .name(workspaceName)
                .ownerUserId(ownerUserId)
                .billingTier(BillingTier.FREE)
                .build();

        workspaceCreateResponse = new WorkspaceCreateResponse(
                savedWorkspace.getId(),
                savedWorkspace.getName(),
                savedWorkspace.getOwnerUserId(),
                savedWorkspace.getBillingTier(),
                Instant.now()
        );
    }

    @Test
    void createWorkspace_whenNameAlreadyExists_throwsInvalidWorkspaceNameException() {
        when(workspaceRepository.existsByNameAndOwnerId(workspaceName, ownerUserId)).thenReturn(true);

        InvalidWorkspaceNameException exception = assertThrows(
                InvalidWorkspaceNameException.class,
                () -> workspaceService.createWorkspace(workspaceName, ownerUserId)
        );

        assertEquals("Workspace with name " + workspaceName + " already exists", exception.getMessage());
        verify(workspaceRepository).existsByNameAndOwnerId(workspaceName, ownerUserId);
        verify(workspaceRepository, never()).save(any(Workspace.class));
        verify(workspaceMemberRepository, never()).save(any(WorkspaceMember.class));
        verifyNoInteractions(workspaceMapper);
    }

    @Test
    void createWorkspace_savesWorkspaceAndOwnerMember_andReturnsMappedResponse() {
        when(workspaceRepository.existsByNameAndOwnerId(workspaceName, ownerUserId)).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(savedWorkspace);
        when(workspaceMapper.toWorkspaceCreateResponse(savedWorkspace)).thenReturn(workspaceCreateResponse);

        WorkspaceCreateResponse result = workspaceService.createWorkspace(workspaceName, ownerUserId);

        InOrder inOrder = inOrder(workspaceRepository, workspaceMemberRepository, workspaceMapper);
        inOrder.verify(workspaceRepository).existsByNameAndOwnerId(workspaceName, ownerUserId);

        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        inOrder.verify(workspaceRepository).save(workspaceCaptor.capture());
        Workspace persistedWorkspace = workspaceCaptor.getValue();
        assertEquals(workspaceName, persistedWorkspace.getName());
        assertEquals(ownerUserId, persistedWorkspace.getOwnerUserId());
        assertEquals(BillingTier.FREE, persistedWorkspace.getBillingTier());

        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        inOrder.verify(workspaceMemberRepository).save(memberCaptor.capture());
        WorkspaceMember savedMember = memberCaptor.getValue();
        assertEquals(savedWorkspace.getId(), savedMember.getWorkspaceId());
        assertEquals(ownerUserId, savedMember.getUserId());
        assertEquals(WorkspaceRole.OWNER, savedMember.getRole());

        inOrder.verify(workspaceMapper).toWorkspaceCreateResponse(savedWorkspace);
        assertEquals(workspaceCreateResponse, result);
        verifyNoMoreInteractions(workspaceRepository, workspaceMemberRepository, workspaceMapper);
    }
}
