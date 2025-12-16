package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.InvitationAction;
import com.baskaaleksander.nuvine.application.dto.UserInternalResponse;
import com.baskaaleksander.nuvine.domain.exception.InvitationEmailMismatchException;
import com.baskaaleksander.nuvine.domain.exception.InvitationTokenExpiredException;
import com.baskaaleksander.nuvine.domain.exception.InvitationTokenNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.UserNotFoundException;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberInviteToken;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberStatus;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import com.baskaaleksander.nuvine.infrastructure.client.AuthClient;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberInviteTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceMemberInviteTokenServiceTest {

    @Mock
    private WorkspaceMemberInviteTokenRepository workspaceMemberInviteTokenRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private WorkspaceMemberInviteTokenService workspaceMemberInviteTokenService;

    private UUID workspaceId;
    private UUID memberId;
    private UUID userId;
    private String email;
    private String token;
    private WorkspaceMember pendingMember;
    private WorkspaceMemberInviteToken validInviteToken;
    private UserInternalResponse userInternalResponse;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        userId = UUID.randomUUID();
        email = "user@example.com";
        token = UUID.randomUUID().toString();

        pendingMember = WorkspaceMember.builder()
                .id(memberId)
                .workspaceId(workspaceId)
                .email(email)
                .role(WorkspaceRole.VIEWER)
                .status(WorkspaceMemberStatus.PENDING)
                .deleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        validInviteToken = WorkspaceMemberInviteToken.builder()
                .id(UUID.randomUUID())
                .token(token)
                .workspaceMember(pendingMember)
                .expiresAt(Instant.now().plusSeconds(3600))
                .usedAt(null)
                .createdAt(Instant.now())
                .build();

        userInternalResponse = new UserInternalResponse(userId, "John", "Doe", email);
    }

    @Test
    void respondToInvitation_whenTokenNotFound_throwsInvitationTokenNotFoundException() {
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThrows(InvitationTokenNotFoundException.class,
                () -> workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.ACCEPT, email));

        verify(workspaceMemberInviteTokenRepository).findByToken(token);
        verifyNoInteractions(workspaceMemberRepository, authClient);
    }

    @Test
    void respondToInvitation_whenTokenAlreadyUsed_throwsInvitationTokenNotFoundException() {
        validInviteToken.setUsedAt(Instant.now().minusSeconds(100));
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.of(validInviteToken));

        assertThrows(InvitationTokenNotFoundException.class,
                () -> workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.ACCEPT, email));

        verify(workspaceMemberInviteTokenRepository).findByToken(token);
        verifyNoInteractions(workspaceMemberRepository, authClient);
    }

    @Test
    void respondToInvitation_whenTokenExpired_throwsInvitationTokenExpiredException() {
        validInviteToken.setExpiresAt(Instant.now().minusSeconds(100));
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.of(validInviteToken));

        assertThrows(InvitationTokenExpiredException.class,
                () -> workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.ACCEPT, email));

        verify(workspaceMemberInviteTokenRepository).findByToken(token);
        verifyNoInteractions(workspaceMemberRepository, authClient);
    }

    @Test
    void respondToInvitation_whenMemberNotPending_throwsInvitationTokenNotFoundException() {
        pendingMember.setStatus(WorkspaceMemberStatus.ACCEPTED);
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.of(validInviteToken));

        assertThrows(InvitationTokenNotFoundException.class,
                () -> workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.ACCEPT, email));

        verify(workspaceMemberInviteTokenRepository).findByToken(token);
        verifyNoInteractions(workspaceMemberRepository, authClient);
    }

    @Test
    void respondToInvitation_whenEmailMismatch_throwsInvitationEmailMismatchException() {
        String differentEmail = "different@example.com";
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.of(validInviteToken));

        assertThrows(InvitationEmailMismatchException.class,
                () -> workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.ACCEPT, differentEmail));

        verify(workspaceMemberInviteTokenRepository).findByToken(token);
        verifyNoInteractions(workspaceMemberRepository, authClient);
    }

    @Test
    void respondToInvitation_whenEmailMismatchCaseInsensitive_doesNotThrow() {
        String upperCaseEmail = email.toUpperCase();
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.of(validInviteToken));
        when(authClient.getUserByEmail(upperCaseEmail)).thenReturn(userInternalResponse);

        workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.ACCEPT, upperCaseEmail);

        verify(authClient).getUserByEmail(upperCaseEmail);
        verify(workspaceMemberRepository).save(pendingMember);
    }

    @Test
    void respondToInvitation_accept_whenUserNotFoundInAuthService_throwsUserNotFoundException() {
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.of(validInviteToken));
        when(authClient.getUserByEmail(email)).thenThrow(new RuntimeException("User not found"));

        assertThrows(UserNotFoundException.class,
                () -> workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.ACCEPT, email));

        verify(workspaceMemberInviteTokenRepository).findByToken(token);
        verify(authClient).getUserByEmail(email);
        verify(workspaceMemberRepository, never()).save(any());
    }

    @Test
    void respondToInvitation_accept_updatesMemberAndMarksTokenUsed() {
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.of(validInviteToken));
        when(authClient.getUserByEmail(email)).thenReturn(userInternalResponse);

        workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.ACCEPT, email);

        assertEquals(userId, pendingMember.getUserId());
        assertEquals("John Doe", pendingMember.getUserName());
        assertEquals(WorkspaceMemberStatus.ACCEPTED, pendingMember.getStatus());
        assertFalse(pendingMember.isDeleted());
        assertNotNull(validInviteToken.getUsedAt());

        verify(workspaceMemberRepository).save(pendingMember);
        verify(workspaceMemberInviteTokenRepository).save(validInviteToken);
    }

    @Test
    void respondToInvitation_decline_updatesMemberAndMarksTokenUsed() {
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.of(validInviteToken));

        workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.DECLINE, email);

        assertNull(pendingMember.getUserId());
        assertEquals(WorkspaceMemberStatus.REJECTED, pendingMember.getStatus());
        assertTrue(pendingMember.isDeleted());
        assertNotNull(validInviteToken.getUsedAt());

        verify(authClient, never()).getUserByEmail(any());
        verify(workspaceMemberRepository).save(pendingMember);
        verify(workspaceMemberInviteTokenRepository).save(validInviteToken);
    }

    @Test
    void respondToInvitation_decline_doesNotCallAuthClient() {
        when(workspaceMemberInviteTokenRepository.findByToken(token)).thenReturn(Optional.of(validInviteToken));

        workspaceMemberInviteTokenService.respondToInvitation(token, InvitationAction.DECLINE, email);

        verifyNoInteractions(authClient);
    }
}
