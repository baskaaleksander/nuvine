package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.WorkspaceMember;
import com.baskaaleksander.nuvine.domain.model.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    @Query("select wm.workspaceId from WorkspaceMember wm where wm.userId = :userId and wm.deleted = false")
    List<UUID> findWorkspaceIdsByUserId(UUID userId);

    @Query("select count(*) from WorkspaceMember wm where wm.workspaceId = :workspaceId and wm.deleted = false")
    Long getWorkspaceMemberCountByWorkspaceId(UUID workspaceId);

    @Query("select count(wm) > 0 from WorkspaceMember wm where wm.workspaceId = :workspaceId and wm.userId = :userId")
    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    @Query("update WorkspaceMember wm set wm.deleted = true where wm.workspaceId = :workspaceId")
    @Modifying
    void deleteAllMembersByWorkspaceId(UUID workspaceId);

    @Query("select wm from WorkspaceMember wm where wm.workspaceId = :workspaceId and wm.deleted = false")
    List<WorkspaceMember> getWorkspaceMembersByWorkspaceId(UUID workspaceId);

    @Query("select wm.userId from WorkspaceMember wm where wm.workspaceId = :workspaceId and wm.deleted = false and wm.role = 'OWNER'")
    Optional<UUID> findOwnerUserIdByWorkspaceId(UUID workspaceId);

    @Query("update WorkspaceMember wm set wm.role = :role where wm.workspaceId = :workspaceId and wm.userId = :userId")
    @Modifying
    void updateMemberRole(UUID userId, UUID workspaceId, WorkspaceRole role);

    @Query("update WorkspaceMember wm set wm.deleted = true where wm.workspaceId = :workspaceId and wm.userId = :userId")
    @Modifying
    int deleteByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
