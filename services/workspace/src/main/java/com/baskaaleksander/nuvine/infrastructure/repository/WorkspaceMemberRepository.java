package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    @Query("select wm.workspaceId from WorkspaceMember wm where wm.userId = :userId")
    List<UUID> findWorkspaceIdsByUserId(UUID userId);

    @Query("select count(*) from WorkspaceMember wm where wm.workspaceId = :workspaceId")
    Long getWorkspaceMemberCountByWorkspaceId(UUID workspaceId);
}
