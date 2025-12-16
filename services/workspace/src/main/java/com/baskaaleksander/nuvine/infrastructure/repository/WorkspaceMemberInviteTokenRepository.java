package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.WorkspaceMemberInviteToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberInviteTokenRepository extends JpaRepository<WorkspaceMemberInviteToken, UUID> {

    Optional<WorkspaceMemberInviteToken> findFirstByWorkspaceMember_IdAndUsedAtIsNullOrderByCreatedAtDesc(UUID workspaceMemberId);

    Optional<WorkspaceMemberInviteToken> findByToken(String token);
}
