package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMembersResponse;
import com.baskaaleksander.nuvine.application.mapper.WorkspaceMemberMapper;
import com.baskaaleksander.nuvine.domain.exception.*;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberInviteToken;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberStatus;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.client.AuthClient;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberAddedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberInvitedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceMemberAddedEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.WorkspaceMemberInvitedEventProducer;
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
    @Mock
    private WorkspaceMemberInvitedEventProducer workspaceMemberInvitedEventProducer;
    @Mock
    private WorkspaceMemberInviteTokenService workspaceMemberInviteTokenService;
    @Mock
    private AccessCacheEvictionService accessCacheEvictionService;
    @Mock
    private EntityCacheEvictionService entityCacheEvictionService;

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
                "user@example.com",
                "Test User",
                activeMember.getRole(),
                WorkspaceMemberStatus.ACCEPTED,
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

        userInternal = new UserInternalResponse(userId, "user@example.com", "user", "user");
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

        InOrder inOrder = inOrder(workspaceMemberRepository, accessCacheEvictionService, entityCacheEvictionService);
        inOrder.verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        inOrder.verify(workspaceMemberRepository).updateDeletedById(deletedMember.getId(), false);
        inOrder.verify(workspaceMemberRepository).updateMemberRole(userId, workspaceId, WorkspaceRole.MODERATOR);
        inOrder.verify(accessCacheEvictionService).evictAccessForUserInWorkspace(workspaceId, userId);
        inOrder.verify(entityCacheEvictionService).evictWorkspaceMember(workspaceId, userId);
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

        InOrder inOrder = inOrder(workspaceMemberRepository, workspaceMemberAddedEventProducer, accessCacheEvictionService, entityCacheEvictionService);
        inOrder.verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        inOrder.verify(workspaceMemberRepository).save(memberCaptor.capture());
        inOrder.verify(workspaceMemberAddedEventProducer).sendWorkspaceMemberAddedEvent(eventCaptor.capture());
        inOrder.verify(accessCacheEvictionService).evictAccessForUserInWorkspace(workspaceId, userId);
        inOrder.verify(entityCacheEvictionService).evictWorkspaceMember(workspaceId, userId);

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

        InOrder inOrder = inOrder(workspaceMemberRepository, accessCacheEvictionService, entityCacheEvictionService);
        inOrder.verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        inOrder.verify(workspaceMemberRepository).findOwnerUserIdByWorkspaceId(workspaceId);
        inOrder.verify(workspaceMemberRepository).updateMemberRole(userId, workspaceId, WorkspaceRole.OWNER);
        inOrder.verify(workspaceMemberRepository).updateMemberRole(ownerId, workspaceId, WorkspaceRole.MODERATOR);
        inOrder.verify(accessCacheEvictionService).evictAccessForUserInWorkspace(workspaceId, userId);
        inOrder.verify(accessCacheEvictionService).evictAccessForUserInWorkspace(workspaceId, ownerId);
        inOrder.verify(entityCacheEvictionService).evictWorkspaceMember(workspaceId, userId);
        inOrder.verify(entityCacheEvictionService).evictWorkspaceMember(workspaceId, ownerId);
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

        InOrder inOrder = inOrder(workspaceMemberRepository, accessCacheEvictionService, entityCacheEvictionService);
        inOrder.verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        inOrder.verify(workspaceMemberRepository).updateMemberRole(userId, workspaceId, WorkspaceRole.VIEWER);
        inOrder.verify(accessCacheEvictionService).evictAccessForUserInWorkspace(workspaceId, userId);
        inOrder.verify(entityCacheEvictionService).evictWorkspaceMember(workspaceId, userId);
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

        InOrder inOrder = inOrder(workspaceMemberRepository, accessCacheEvictionService, entityCacheEvictionService);
        inOrder.verify(workspaceMemberRepository).findByWorkspaceIdAndUserId(workspaceId, userId);
        inOrder.verify(workspaceMemberRepository).updateDeletedById(activeMember.getId(), true);
        inOrder.verify(accessCacheEvictionService).evictAccessForUserInWorkspace(workspaceId, userId);
        inOrder.verify(entityCacheEvictionService).evictWorkspaceMember(workspaceId, userId);
    }

    @Test
    void inviteWorkspaceMember_whenNewInvite_createsTokenAndSendsEvent() {
        String email = "invitee@example.com";
        WorkspaceRole role = WorkspaceRole.VIEWER;

        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test workspace")
                .ownerUserId(ownerId)
                .deleted(false)
                .build();

        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndEmail(workspaceId, email)).thenReturn(java.util.Optional.empty());
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> {
            WorkspaceMember member = invocation.getArgument(0);
            if (member.getId() == null) {
                member.setId(UUID.randomUUID());
            }
            return member;
        });

        WorkspaceMemberInviteToken inviteToken = WorkspaceMemberInviteToken.builder()
                .token("token-1")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        ArgumentCaptor<WorkspaceMember> tokenMemberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        when(workspaceMemberInviteTokenService.getOrCreateActiveToken(tokenMemberCaptor.capture(), eq(true)))
                .thenReturn(inviteToken);

        ArgumentCaptor<WorkspaceMemberInvitedEvent> eventCaptor = ArgumentCaptor.forClass(WorkspaceMemberInvitedEvent.class);

        workspaceMemberService.inviteWorkspaceMember(workspaceId, email, role);

        WorkspaceMember tokenMember = tokenMemberCaptor.getValue();
        assertEquals(workspaceId, tokenMember.getWorkspaceId());
        assertEquals(email, tokenMember.getEmail());
        assertEquals(role, tokenMember.getRole());
        assertEquals(WorkspaceMemberStatus.PENDING, tokenMember.getStatus());
        assertEquals(false, tokenMember.isDeleted());

        verify(workspaceMemberInvitedEventProducer).sendWorkspaceMemberInvitedEvent(eventCaptor.capture());
        assertEquals(new WorkspaceMemberInvitedEvent(
                email,
                workspaceId.toString(),
                workspace.getName(),
                role.toString(),
                inviteToken.getToken()
        ), eventCaptor.getValue());
    }

    @Test
    void inviteWorkspaceMember_whenPendingMemberExists_reusesTokenAndUpdatesRole() {
        String email = "invitee@example.com";
        WorkspaceRole role = WorkspaceRole.VIEWER;

        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test workspace")
                .ownerUserId(ownerId)
                .deleted(false)
                .build();

        WorkspaceMember pendingMember = WorkspaceMember.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .email(email)
                .role(WorkspaceRole.MODERATOR)
                .status(WorkspaceMemberStatus.PENDING)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndEmail(workspaceId, email)).thenReturn(java.util.Optional.of(pendingMember));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceMemberInviteToken inviteToken = WorkspaceMemberInviteToken.builder()
                .token("token-reused")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        ArgumentCaptor<WorkspaceMember> tokenMemberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        when(workspaceMemberInviteTokenService.getOrCreateActiveToken(tokenMemberCaptor.capture(), eq(false)))
                .thenReturn(inviteToken);

        ArgumentCaptor<WorkspaceMemberInvitedEvent> eventCaptor = ArgumentCaptor.forClass(WorkspaceMemberInvitedEvent.class);

        workspaceMemberService.inviteWorkspaceMember(workspaceId, email, role);

        WorkspaceMember tokenMember = tokenMemberCaptor.getValue();
        assertEquals(pendingMember.getId(), tokenMember.getId());
        assertEquals(role, tokenMember.getRole());
        assertEquals(WorkspaceMemberStatus.PENDING, tokenMember.getStatus());
        assertEquals(false, tokenMember.isDeleted());

        verify(workspaceMemberInvitedEventProducer).sendWorkspaceMemberInvitedEvent(eventCaptor.capture());
        assertEquals(new WorkspaceMemberInvitedEvent(
                email,
                workspaceId.toString(),
                workspace.getName(),
                role.toString(),
                inviteToken.getToken()
        ), eventCaptor.getValue());
    }

    @Test
    void inviteWorkspaceMember_whenMemberRejected_forcesNewToken() {
        String email = "invitee@example.com";
        WorkspaceRole role = WorkspaceRole.VIEWER;

        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test workspace")
                .ownerUserId(ownerId)
                .deleted(false)
                .build();

        WorkspaceMember rejectedMember = WorkspaceMember.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .email(email)
                .role(WorkspaceRole.MODERATOR)
                .status(WorkspaceMemberStatus.REJECTED)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndEmail(workspaceId, email)).thenReturn(java.util.Optional.of(rejectedMember));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceMemberInviteToken inviteToken = WorkspaceMemberInviteToken.builder()
                .token("token-new")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        ArgumentCaptor<WorkspaceMember> tokenMemberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        when(workspaceMemberInviteTokenService.getOrCreateActiveToken(tokenMemberCaptor.capture(), eq(true)))
                .thenReturn(inviteToken);

        workspaceMemberService.inviteWorkspaceMember(workspaceId, email, role);

        WorkspaceMember tokenMember = tokenMemberCaptor.getValue();
        assertEquals(WorkspaceMemberStatus.PENDING, tokenMember.getStatus());
        assertEquals(false, tokenMember.isDeleted());
    }

    @Test
    void inviteWorkspaceMember_whenAcceptedMemberExists_throwsWorkspaceMemberExistsException() {
        String email = "invitee@example.com";

        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test workspace")
                .ownerUserId(ownerId)
                .deleted(false)
                .build();

        WorkspaceMember acceptedMember = WorkspaceMember.builder()
                .id(UUID.randomUUID())
                .workspaceId(workspaceId)
                .email(email)
                .role(WorkspaceRole.VIEWER)
                .status(WorkspaceMemberStatus.ACCEPTED)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(workspaceRepository.findById(workspaceId)).thenReturn(java.util.Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndEmail(workspaceId, email)).thenReturn(java.util.Optional.of(acceptedMember));

        assertThrows(WorkspaceMemberExistsException.class, () ->
                workspaceMemberService.inviteWorkspaceMember(workspaceId, email, WorkspaceRole.VIEWER));

        verifyNoInteractions(workspaceMemberInviteTokenService, workspaceMemberInvitedEventProducer);
        verify(workspaceMemberRepository, never()).save(any());
    }
}
