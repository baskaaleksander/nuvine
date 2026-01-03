package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMapper;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMemberMapper;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.exception.InvalidWorkspaceNameException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import com.baskaaleksander.nuvine.domain.model.BillingTier;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberStatus;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.client.AuthClient;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
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
    @Mock
    private AuthClient authClient;

    @InjectMocks
    private WorkspaceService workspaceService;

    private UUID ownerUserId;
    private String workspaceName;
    private Workspace savedWorkspace;
    private WorkspaceCreateResponse workspaceCreateResponse;
    private UUID workspaceId;
    private Workspace activeWorkspace;
    private Workspace deletedWorkspace;
    private WorkspaceResponse workspaceResponse;
    private UUID memberUserId;
    private WorkspaceMember activeMember;
    private WorkspaceMember deletedMember;
    private WorkspaceMemberResponse memberResponse;
    private String updatedWorkspaceName;
    private UserInternalResponse userInternalResponse;

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

        workspaceId = UUID.randomUUID();

        activeWorkspace = Workspace.builder()
                .id(workspaceId)
                .name(workspaceName)
                .ownerUserId(ownerUserId)
                .billingTier(BillingTier.FREE)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        deletedWorkspace = Workspace.builder()
                .id(UUID.randomUUID())
                .name("Deleted Workspace")
                .ownerUserId(ownerUserId)
                .billingTier(BillingTier.FREE)
                .deleted(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        workspaceResponse = new WorkspaceResponse(
                activeWorkspace.getId(),
                activeWorkspace.getName(),
                activeWorkspace.getOwnerUserId(),
                activeWorkspace.getSubscriptionId(),
                activeWorkspace.getBillingTier(),
                activeWorkspace.getCreatedAt(),
                activeWorkspace.getUpdatedAt()
        );

        memberUserId = UUID.randomUUID();
        activeMember = WorkspaceMember.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .userId(memberUserId)
                .role(WorkspaceRole.MEMBER)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        deletedMember = WorkspaceMember.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .userId(memberUserId)
                .role(WorkspaceRole.MEMBER)
                .deleted(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        memberResponse = new WorkspaceMemberResponse(
                activeMember.getId(),
                activeMember.getWorkspaceId(),
                activeMember.getUserId(),
                "user@example.com",
                "Test User",
                activeMember.getRole(),
                WorkspaceMemberStatus.ACCEPTED,
                activeMember.getCreatedAt()
        );

        updatedWorkspaceName = "Updated Workspace";

        userInternalResponse = new UserInternalResponse(
                ownerUserId,
                "firstName",
                "lastName",
                "user@example.com"
        );
    }

    @Test
    void createWorkspace_whenNameAlreadyExists_throwsInvalidWorkspaceNameException() {
        when(workspaceRepository.existsByNameAndOwnerId(workspaceName, ownerUserId)).thenReturn(true);

        InvalidWorkspaceNameException exception = assertThrows(
                InvalidWorkspaceNameException.class,
                () -> workspaceService.createWorkspace(workspaceName, ownerUserId, "placeholder")
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
        when(authClient.getUserByEmail("user@example.com")).thenReturn(userInternalResponse);

        WorkspaceCreateResponse result = workspaceService.createWorkspace(workspaceName, ownerUserId, "user@example.com");

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

    @Test
    void getWorkspaces_filtersDeletedAndMapsActive_withPaginationMeta() {
        PaginationRequest request = new PaginationRequest(0, 5, "name", Sort.Direction.ASC);
        Pageable expectedPageable = PaginationUtil.getPageable(request);

        List<UUID> workspaceIds = List.of(activeWorkspace.getId(), deletedWorkspace.getId());
        Page<Workspace> page = new PageImpl<>(List.of(activeWorkspace, deletedWorkspace), expectedPageable, workspaceIds.size());

        when(workspaceMemberRepository.findWorkspaceIdsByUserId(ownerUserId)).thenReturn(workspaceIds);
        when(workspaceRepository.findAllByIdIn(workspaceIds, expectedPageable)).thenReturn(page);
        when(workspaceMapper.toWorkspaceResponse(activeWorkspace)).thenReturn(workspaceResponse);

        PagedResponse<WorkspaceResponse> response = workspaceService.getWorkspaces(ownerUserId, request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(workspaceRepository).findAllByIdIn(eq(workspaceIds), pageableCaptor.capture());
        assertEquals(expectedPageable, pageableCaptor.getValue());

        assertEquals(1, response.content().size());
        assertEquals(workspaceResponse, response.content().iterator().next());
        assertEquals(page.getTotalPages(), response.totalPages());
        assertEquals(page.getTotalElements(), response.totalElements());
        assertEquals(page.getSize(), response.size());
        assertEquals(page.getNumber(), response.page());
        assertEquals(page.isLast(), response.last());
        assertEquals(page.hasNext(), response.next());
        verify(workspaceMapper, times(1)).toWorkspaceResponse(activeWorkspace);
        verifyNoMoreInteractions(workspaceMapper);
    }

    @Test
    void getWorkspaces_whenNoMemberships_returnsEmptyPagedResponse() {
        PaginationRequest request = new PaginationRequest(0, 5, "name", Sort.Direction.ASC);
        Pageable expectedPageable = PaginationUtil.getPageable(request);

        when(workspaceMemberRepository.findWorkspaceIdsByUserId(ownerUserId)).thenReturn(List.of());
        when(workspaceRepository.findAllByIdIn(List.of(), expectedPageable))
                .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

        PagedResponse<WorkspaceResponse> response = workspaceService.getWorkspaces(ownerUserId, request);

        assertEquals(0, response.content().size());
        assertEquals(0, response.totalElements());
        assertEquals(0, response.totalPages());
        verifyNoInteractions(workspaceMapper);
    }

    @Test
    void getWorkspace_whenNotFound_throwsWorkspaceNotFoundException() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.empty());

        assertThrows(WorkspaceNotFoundException.class, () -> workspaceService.getWorkspace(workspaceId));

        verify(workspaceRepository).findById(workspaceId);
        verifyNoInteractions(workspaceMemberRepository, projectRepository);
    }

    @Test
    void getWorkspace_whenDeleted_throwsWorkspaceNotFoundException() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.of(deletedWorkspace));

        assertThrows(WorkspaceNotFoundException.class, () -> workspaceService.getWorkspace(workspaceId));

        verify(workspaceRepository).findById(workspaceId);
        verifyNoInteractions(workspaceMemberRepository, projectRepository);
    }

    @Test
    void getWorkspace_returnsWorkspaceWithStats() {
        WorkspaceResponseWithStats expectedResponse = new WorkspaceResponseWithStats(
                activeWorkspace.getId(),
                activeWorkspace.getName(),
                activeWorkspace.getOwnerUserId(),
                activeWorkspace.getSubscriptionId(),
                activeWorkspace.getBillingTier(),
                activeWorkspace.getCreatedAt(),
                activeWorkspace.getUpdatedAt(),
                3L,
                7L
        );

        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.of(activeWorkspace));
        when(workspaceMemberRepository.getWorkspaceMemberCountByWorkspaceId(workspaceId)).thenReturn(3L);
        when(projectRepository.getProjectCountByWorkspaceId(workspaceId)).thenReturn(7L);

        WorkspaceResponseWithStats response = workspaceService.getWorkspace(workspaceId);

        assertEquals(expectedResponse, response);
        verify(workspaceRepository).findById(workspaceId);
        verify(workspaceMemberRepository).getWorkspaceMemberCountByWorkspaceId(workspaceId);
        verify(projectRepository).getProjectCountByWorkspaceId(workspaceId);
    }

    @Test
    void updateWorkspace_whenNameAlreadyExists_throwsInvalidWorkspaceNameException() {
        when(workspaceRepository.existsByNameAndOwnerId(updatedWorkspaceName, ownerUserId)).thenReturn(true);

        assertThrows(InvalidWorkspaceNameException.class,
                () -> workspaceService.updateWorkspace(workspaceId, updatedWorkspaceName, ownerUserId));

        verify(workspaceRepository).existsByNameAndOwnerId(updatedWorkspaceName, ownerUserId);
        verify(workspaceRepository, never()).findById(any());
        verify(workspaceRepository, never()).updateWorkspaceName(any(), any());
    }

    @Test
    void updateWorkspace_whenWorkspaceNotFound_throwsWorkspaceNotFoundException() {
        when(workspaceRepository.existsByNameAndOwnerId(updatedWorkspaceName, ownerUserId)).thenReturn(false);
        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.empty());

        assertThrows(WorkspaceNotFoundException.class,
                () -> workspaceService.updateWorkspace(workspaceId, updatedWorkspaceName, ownerUserId));

        verify(workspaceRepository).existsByNameAndOwnerId(updatedWorkspaceName, ownerUserId);
        verify(workspaceRepository).findById(workspaceId);
        verify(workspaceRepository, never()).updateWorkspaceName(any(), any());
    }

    @Test
    void updateWorkspace_whenWorkspaceDeleted_throwsWorkspaceNotFoundException() {
        when(workspaceRepository.existsByNameAndOwnerId(updatedWorkspaceName, ownerUserId)).thenReturn(false);
        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.of(deletedWorkspace));

        assertThrows(WorkspaceNotFoundException.class,
                () -> workspaceService.updateWorkspace(workspaceId, updatedWorkspaceName, ownerUserId));

        verify(workspaceRepository).existsByNameAndOwnerId(updatedWorkspaceName, ownerUserId);
        verify(workspaceRepository).findById(workspaceId);
        verify(workspaceRepository, never()).updateWorkspaceName(any(), any());
    }

    @Test
    void updateWorkspace_updatesNameWhenValid() {
        when(workspaceRepository.existsByNameAndOwnerId(updatedWorkspaceName, ownerUserId)).thenReturn(false);
        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.of(activeWorkspace));

        workspaceService.updateWorkspace(workspaceId, updatedWorkspaceName, ownerUserId);

        InOrder inOrder = inOrder(workspaceRepository);
        inOrder.verify(workspaceRepository).existsByNameAndOwnerId(updatedWorkspaceName, ownerUserId);
        inOrder.verify(workspaceRepository).findById(workspaceId);
        inOrder.verify(workspaceRepository).updateWorkspaceName(workspaceId, updatedWorkspaceName);
        verifyNoMoreInteractions(workspaceRepository);
        verifyNoInteractions(workspaceMemberRepository, projectRepository, workspaceMapper, workspaceMemberMapper);
    }

    @Test
    void getSelfWorkspaceMember_whenNotFound_throwsWorkspaceMemberNotFoundException() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, memberUserId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(WorkspaceMemberNotFoundException.class,
                () -> workspaceService.getSelfWorkspaceMember(workspaceId, memberUserId));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, memberUserId);
        verifyNoInteractions(workspaceMemberMapper);
    }

    @Test
    void getSelfWorkspaceMember_whenDeleted_throwsWorkspaceMemberNotFoundException() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, memberUserId))
                .thenReturn(java.util.Optional.of(deletedMember));

        assertThrows(WorkspaceMemberNotFoundException.class,
                () -> workspaceService.getSelfWorkspaceMember(workspaceId, memberUserId));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, memberUserId);
        verifyNoInteractions(workspaceMemberMapper);
    }

    @Test
    void getSelfWorkspaceMember_returnsMappedResponse() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, memberUserId))
                .thenReturn(java.util.Optional.of(activeMember));
        when(workspaceMemberMapper.toWorkspaceMemberResponse(activeMember)).thenReturn(memberResponse);

        WorkspaceMemberResponse response = workspaceService.getSelfWorkspaceMember(workspaceId, memberUserId);

        assertEquals(memberResponse, response);
        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, memberUserId);
        verify(workspaceMemberMapper).toWorkspaceMemberResponse(activeMember);
    }

    @Test
    void deleteWorkspace_whenNotFound_throwsWorkspaceNotFoundException() {
        when(workspaceRepository.existsById(workspaceId)).thenReturn(false);

        assertThrows(WorkspaceNotFoundException.class, () -> workspaceService.deleteWorkspace(workspaceId));

        verify(workspaceRepository).existsById(workspaceId);
        verify(workspaceMemberRepository, never()).deleteAllMembersByWorkspaceId(any());
        verify(workspaceRepository, never()).deleteWorkspace(any());
    }

    @Test
    void deleteWorkspace_deletesMembersThenWorkspace() {
        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);

        workspaceService.deleteWorkspace(workspaceId);

        InOrder inOrder = inOrder(workspaceMemberRepository, workspaceRepository);
        inOrder.verify(workspaceRepository).existsById(workspaceId);
        inOrder.verify(workspaceMemberRepository).deleteAllMembersByWorkspaceId(workspaceId);
        inOrder.verify(workspaceRepository).deleteWorkspace(workspaceId);
        verifyNoMoreInteractions(workspaceMemberRepository, workspaceRepository);
    }
}
