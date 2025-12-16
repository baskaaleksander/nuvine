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
import com.baskaaleksander.nuvine.infrastructure.client.AuthClient;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberInviteTokenRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceMemberInviteTokenService {

    private static final long EXPIRATION_TIME_SECONDS = 7 * 24 * 3600;

    private final WorkspaceMemberInviteTokenRepository workspaceMemberInviteTokenRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AuthClient authClient;

    public WorkspaceMemberInviteToken getOrCreateActiveToken(WorkspaceMember workspaceMember, boolean forceNew) {
        log.info("GET_OR_CREATE_WORKSPACE_MEMBER_INVITE_TOKEN START workspaceMemberId={} forceNew={}", workspaceMember.getId(), forceNew);

        Optional<WorkspaceMemberInviteToken> latestToken = workspaceMemberInviteTokenRepository
                .findFirstByWorkspaceMember_IdAndUsedAtIsNullOrderByCreatedAtDesc(workspaceMember.getId());

        if (!forceNew) {
            WorkspaceMemberInviteToken existingToken = latestToken.orElse(null);
            if (existingToken != null && existingToken.getExpiresAt().isAfter(Instant.now())) {
                log.info("GET_OR_CREATE_WORKSPACE_MEMBER_INVITE_TOKEN REUSED workspaceMemberId={} tokenId={}", workspaceMember.getId(), existingToken.getId());
                return existingToken;
            }
        }

        UUID token = UUID.randomUUID();
        WorkspaceMemberInviteToken inviteToken = WorkspaceMemberInviteToken.builder()
                .workspaceMember(workspaceMember)
                .token(token.toString())
                .expiresAt(Instant.now().plusSeconds(EXPIRATION_TIME_SECONDS))
                .build();

        WorkspaceMemberInviteToken saved = workspaceMemberInviteTokenRepository.save(inviteToken);
        log.info("GET_OR_CREATE_WORKSPACE_MEMBER_INVITE_TOKEN CREATED workspaceMemberId={} tokenId={}", workspaceMember.getId(), saved.getId());
        return saved;
    }

    @Transactional
    public void respondToInvitation(String token, InvitationAction action, String authenticatedUserEmail) {
        log.info("RESPOND_TO_INVITATION START action={}", action);

        WorkspaceMemberInviteToken inviteToken = workspaceMemberInviteTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.info("RESPOND_TO_INVITATION FAILED reason=token_not_found");
                    return new InvitationTokenNotFoundException("Invitation token not found");
                });

        if (inviteToken.getUsedAt() != null) {
            log.info("RESPOND_TO_INVITATION FAILED reason=token_already_used tokenId={}", inviteToken.getId());
            throw new InvitationTokenNotFoundException("Invitation token has already been used");
        }

        if (inviteToken.getExpiresAt().isBefore(Instant.now())) {
            log.info("RESPOND_TO_INVITATION FAILED reason=token_expired tokenId={}", inviteToken.getId());
            throw new InvitationTokenExpiredException("Invitation token has expired");
        }

        WorkspaceMember member = inviteToken.getWorkspaceMember();

        if (member.getStatus() != WorkspaceMemberStatus.PENDING) {
            log.info("RESPOND_TO_INVITATION FAILED reason=member_not_pending workspaceMemberId={}", member.getId());
            throw new InvitationTokenNotFoundException("Invitation is no longer valid");
        }

        if (!member.getEmail().equalsIgnoreCase(authenticatedUserEmail)) {
            log.info("RESPOND_TO_INVITATION FAILED reason=email_mismatch workspaceMemberId={}", member.getId());
            throw new InvitationEmailMismatchException("Email does not match the invitation");
        }

        if (action == InvitationAction.ACCEPT) {
            UserInternalResponse user;
            try {
                user = authClient.getUserByEmail(authenticatedUserEmail);
            } catch (Exception ex) {
                log.info("RESPOND_TO_INVITATION FAILED reason=user_not_found email={}", authenticatedUserEmail);
                throw new UserNotFoundException("User not found");
            }

            member.setUserId(user.id());
            member.setUserName(user.firstName() + " " + user.lastName());
            member.setStatus(WorkspaceMemberStatus.ACCEPTED);

            log.info("RESPOND_TO_INVITATION ACCEPTED workspaceMemberId={} userId={}", member.getId(), user.id());
        } else {
            member.setStatus(WorkspaceMemberStatus.REJECTED);
            member.setDeleted(true);

            log.info("RESPOND_TO_INVITATION DECLINED workspaceMemberId={}", member.getId());
        }

        inviteToken.setUsedAt(Instant.now());

        workspaceMemberRepository.save(member);
        workspaceMemberInviteTokenRepository.save(inviteToken);

        log.info("RESPOND_TO_INVITATION END action={}", action);
    }
}
