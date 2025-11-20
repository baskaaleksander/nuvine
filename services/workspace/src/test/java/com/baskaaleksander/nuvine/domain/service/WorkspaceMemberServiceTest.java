package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMembersResponse;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMemberMapper;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberExistsException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceMemberNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceRoleConflictException;
import com.baskaaleksander.nuvine.domain.exception.WorkspaceOwnerRemovalException;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.client.AuthClient;
import com.baskaaleksander.nuvine.infrastructure.messaging.WorkspaceMemberAddedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberAddedEvent;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceMemberServiceTest {

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private WorkspaceMemberMapper workspaceMemberMapper;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private AuthClient authClient;
    @Mock
    private WorkspaceMemberAddedEventProducer workspaceMemberAddedEventProducer;

    @InjectMocks
    private WorkspaceMemberService workspaceMemberService;

    private UUID workspaceId;
    private UUID userId;
    private WorkspaceMember activeMember;
    private WorkspaceMember deletedMember;
    private WorkspaceMember ownerMember;
    private WorkspaceMemberResponse memberResponse;
    private UserInternalResponse userInternal;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        activeMember = WorkspaceMember.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .userId(userId)
                .role(WorkspaceRole.MODERATOR)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        deletedMember = WorkspaceMember.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .userId(userId)
                .role(WorkspaceRole.VIEWER)
                .deleted(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        memberResponse = new WorkspaceMemberResponse(
                activeMember.getId(),
                activeMember.getWorkspaceId(),
                activeMember.getUserId(),
                activeMember.getRole(),
                activeMember.getCreatedAt()
        );

        ownerMember = WorkspaceMember.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .userId(ownerId)
                .role(WorkspaceRole.OWNER)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userInternal = new UserInternalResponse(userId.toString(), "user@example.com");
    }

    @Test
    void getWorkspaceMembers_whenWorkspaceNotFound_throwsWorkspaceNotFoundException() {
        when(workspaceRepository.existsById(workspaceId)).thenReturn(false);

        assertThrows(WorkspaceNotFoundException.class, () -> workspaceMemberService.getWorkspaceMembers(workspaceId));

        verify(workspaceRepository).existsById(workspaceId);
        verifyNoInteractions(workspaceMemberRepository, workspaceMemberMapper);
    }

    @Test
    void getWorkspaceMembers_returnsMappedMembersAndCount() {
        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(workspaceMemberRepository.getWorkspaceMemberCountByWorkspaceId(workspaceId)).thenReturn(2L);
        when(workspaceMemberRepository.getWorkspaceMembersByWorkspaceId(workspaceId))
                .thenReturn(List.of(activeMember));
        when(workspaceMemberMapper.toWorkspaceMemberResponse(activeMember)).thenReturn(memberResponse);

        WorkspaceMembersResponse response = workspaceMemberService.getWorkspaceMembers(workspaceId);

        assertEquals(1, response.members().size());
        assertEquals(memberResponse, response.members().get(0));
        assertEquals(2L, response.count());
        verify(workspaceRepository).existsById(workspaceId);
        verify(workspaceMemberRepository).getWorkspaceMemberCountByWorkspaceId(workspaceId);
        verify(workspaceMemberRepository).getWorkspaceMembersByWorkspaceId(workspaceId);
        verify(workspaceMemberMapper).toWorkspaceMemberResponse(activeMember);
        verifyNoMoreInteractions(workspaceMemberMapper);
    }

    @Test
    void getWorkspaceMembers_whenEmpty_returnsEmptyListAndZeroCount() {
        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(workspaceMemberRepository.getWorkspaceMemberCountByWorkspaceId(workspaceId)).thenReturn(0L);
        when(workspaceMemberRepository.getWorkspaceMembersByWorkspaceId(workspaceId)).thenReturn(List.of());

        WorkspaceMembersResponse response = workspaceMemberService.getWorkspaceMembers(workspaceId);

        assertEquals(0, response.members().size());
        assertEquals(0L, response.count());
        verifyNoInteractions(workspaceMemberMapper);
    }

    @Test
    void addWorkspaceMember_whenAuthClientFails_throwsUserNotFoundException() {
        when(authClient.checkInternalUser(userId)).thenThrow(new RuntimeException("auth error"));

        assertThrows(UserNotFoundException.class,
                () -> workspaceMemberService.addWorkspaceMember(workspaceId, userId, WorkspaceRole.MODERATOR));

        verify(authClient).checkInternalUser(userId);
        verifyNoInteractions(workspaceMemberRepository, workspaceMemberAddedEventProducer);
    }

    @Test
    void addWorkspaceMember_withOwnerRole_throwsWorkspaceRoleConflictException() {
        when(authClient.checkInternalUser(userId)).thenReturn(userInternal);

        assertThrows(WorkspaceRoleConflictException.class,
                () -> workspaceMemberService.addWorkspaceMember(workspaceId, userId, WorkspaceRole.OWNER));

        verify(authClient).checkInternalUser(userId);
        verifyNoInteractions(workspaceMemberRepository, workspaceMemberAddedEventProducer);
    }

    @Test
    void addWorkspaceMember_whenActiveMemberExists_throwsWorkspaceMemberExistsException() {
        when(authClient.checkInternalUser(userId)).thenReturn(userInternal);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(activeMember));

        assertThrows(WorkspaceMemberExistsException.class,
                () -> workspaceMemberService.addWorkspaceMember(workspaceId, userId, WorkspaceRole.MODERATOR));

        verify(authClient).checkInternalUser(userId);
        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verifyNoInteractions(workspaceMemberAddedEventProducer);
        verify(workspaceMemberRepository, never()).save(any());
    }

    @Test
    void addWorkspaceMember_whenMemberWasDeleted_reactivatesAndUpdatesRole() {
        when(authClient.checkInternalUser(userId)).thenReturn(userInternal);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(deletedMember));

        workspaceMemberService.addWorkspaceMember(workspaceId, userId, WorkspaceRole.MODERATOR);

        InOrder inOrder = inOrder(workspaceMemberRepository);
        inOrder.verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        inOrder.verify(workspaceMemberRepository).updateDeletedById(deletedMember.getId(), false);
        inOrder.verify(workspaceMemberRepository).updateMemberRole(userId, workspaceId, WorkspaceRole.MODERATOR);
        verifyNoInteractions(workspaceMemberAddedEventProducer);
        verify(workspaceMemberRepository, never()).save(any());
    }

    @Test
    void addWorkspaceMember_savesNewMemberAndSendsEvent() {
        when(authClient.checkInternalUser(userId)).thenReturn(userInternal);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.empty());

        workspaceMemberService.addWorkspaceMember(workspaceId, userId, WorkspaceRole.VIEWER);

        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        ArgumentCaptor<WorkspaceMemberAddedEvent> eventCaptor = ArgumentCaptor.forClass(WorkspaceMemberAddedEvent.class);

        InOrder inOrder = inOrder(workspaceMemberRepository, workspaceMemberAddedEventProducer);
        inOrder.verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        inOrder.verify(workspaceMemberRepository).save(memberCaptor.capture());
        inOrder.verify(workspaceMemberAddedEventProducer).sendWorkspaceMemberAddedEvent(eventCaptor.capture());

        WorkspaceMember savedMember = memberCaptor.getValue();
        assertEquals(workspaceId, savedMember.getWorkspaceId());
        assertEquals(userId, savedMember.getUserId());
        assertEquals(WorkspaceRole.VIEWER, savedMember.getRole());
        assertEquals(new WorkspaceMemberAddedEvent(
                userInternal.email(),
                userId.toString(),
                workspaceId.toString(),
                WorkspaceRole.VIEWER.toString()
        ), eventCaptor.getValue());
    }

    @Test
    void updateWorkspaceMemberRole_whenMemberNotFound_throwsWorkspaceMemberNotFoundException() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(WorkspaceMemberNotFoundException.class,
                () -> workspaceMemberService.updateWorkspaceMemberRole(workspaceId, userId, WorkspaceRole.MODERATOR));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository, never()).updateMemberRole(any(), any(), any());
    }

    @Test
    void updateWorkspaceMemberRole_whenMemberDeleted_throwsWorkspaceMemberNotFoundException() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(deletedMember));

        assertThrows(WorkspaceMemberNotFoundException.class,
                () -> workspaceMemberService.updateWorkspaceMemberRole(workspaceId, userId, WorkspaceRole.MODERATOR));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository, never()).updateMemberRole(any(), any(), any());
    }

    @Test
    void updateWorkspaceMemberRole_toOwner_whenOwnerMissing_throwsWorkspaceMemberNotFoundException() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(activeMember));
        when(workspaceMemberRepository.findOwnerUserIdByWorkspaceId(workspaceId)).thenReturn(java.util.Optional.empty());

        assertThrows(WorkspaceMemberNotFoundException.class,
                () -> workspaceMemberService.updateWorkspaceMemberRole(workspaceId, userId, WorkspaceRole.OWNER));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository).findOwnerUserIdByWorkspaceId(workspaceId);
        verify(workspaceMemberRepository, never()).updateMemberRole(any(), any(), any());
    }

    @Test
    void updateWorkspaceMemberRole_toOwner_whenUserAlreadyOwner_throwsConflict() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(ownerMember));
        when(workspaceMemberRepository.findOwnerUserIdByWorkspaceId(workspaceId)).thenReturn(java.util.Optional.of(userId));

        assertThrows(WorkspaceRoleConflictException.class,
                () -> workspaceMemberService.updateWorkspaceMemberRole(workspaceId, userId, WorkspaceRole.OWNER));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository).findOwnerUserIdByWorkspaceId(workspaceId);
        verify(workspaceMemberRepository, never()).updateMemberRole(any(), any(), any());
    }

    @Test
    void updateWorkspaceMemberRole_promotesToOwner_andDowngradesPreviousOwner() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(activeMember));
        when(workspaceMemberRepository.findOwnerUserIdByWorkspaceId(workspaceId)).thenReturn(java.util.Optional.of(ownerId));

        workspaceMemberService.updateWorkspaceMemberRole(workspaceId, userId, WorkspaceRole.OWNER);

        InOrder inOrder = inOrder(workspaceMemberRepository);
        inOrder.verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        inOrder.verify(workspaceMemberRepository).findOwnerUserIdByWorkspaceId(workspaceId);
        inOrder.verify(workspaceMemberRepository).updateMemberRole(userId, workspaceId, WorkspaceRole.OWNER);
        inOrder.verify(workspaceMemberRepository).updateMemberRole(ownerId, workspaceId, WorkspaceRole.MODERATOR);
    }

    @Test
    void updateWorkspaceMemberRole_whenMemberIsOwner_andDowngradeAttempt_throwsConflict() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(ownerMember));

        assertThrows(WorkspaceRoleConflictException.class,
                () -> workspaceMemberService.updateWorkspaceMemberRole(workspaceId, userId, WorkspaceRole.MODERATOR));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository, never()).updateMemberRole(any(), any(), any());
    }

    @Test
    void updateWorkspaceMemberRole_whenRoleUnchanged_throwsConflict() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(activeMember));

        assertThrows(WorkspaceRoleConflictException.class,
                () -> workspaceMemberService.updateWorkspaceMemberRole(workspaceId, userId, activeMember.getRole()));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository, never()).updateMemberRole(any(), any(), any());
    }

    @Test
    void updateWorkspaceMemberRole_updatesRoleWhenValid() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(activeMember));

        workspaceMemberService.updateWorkspaceMemberRole(workspaceId, userId, WorkspaceRole.VIEWER);

        InOrder inOrder = inOrder(workspaceMemberRepository);
        inOrder.verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        inOrder.verify(workspaceMemberRepository).updateMemberRole(userId, workspaceId, WorkspaceRole.VIEWER);
    }

    @Test
    void removeWorkspaceMember_whenNotFound_throwsWorkspaceMemberNotFoundException() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(WorkspaceMemberNotFoundException.class,
                () -> workspaceMemberService.removeWorkspaceMember(workspaceId, userId));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository, never()).updateDeletedById(any(), anyBoolean());
    }

    @Test
    void removeWorkspaceMember_whenAlreadyDeleted_throwsWorkspaceMemberNotFoundException() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(deletedMember));

        assertThrows(WorkspaceMemberNotFoundException.class,
                () -> workspaceMemberService.removeWorkspaceMember(workspaceId, userId));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository, never()).updateDeletedById(any(), anyBoolean());
    }

    @Test
    void removeWorkspaceMember_whenOwner_throwsWorkspaceOwnerRemovalException() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(ownerMember));

        assertThrows(WorkspaceOwnerRemovalException.class,
                () -> workspaceMemberService.removeWorkspaceMember(workspaceId, userId));

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository, never()).updateDeletedById(any(), anyBoolean());
    }

    @Test
    void removeWorkspaceMember_setsDeletedTrue() {
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(java.util.Optional.of(activeMember));

        workspaceMemberService.removeWorkspaceMember(workspaceId, userId);

        verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        verify(workspaceMemberRepository).updateDeletedById(activeMember.getId(), true);
    }
}
