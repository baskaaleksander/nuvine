package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberInviteToken;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceMemberInviteTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceMemberInviteTokenService {

    private static final long EXPIRATION_TIME_SECONDS = 7 * 24 * 3600;

    private final WorkspaceMemberInviteTokenRepository workspaceMemberInviteTokenRepository;

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
}
