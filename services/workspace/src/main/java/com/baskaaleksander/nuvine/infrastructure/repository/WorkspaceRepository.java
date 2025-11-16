package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("SELECT COUNT(w) > 0 FROM Workspace w WHERE w.name = :name AND w.ownerUserId = :userId and w.deleted = false")
    boolean existsByNameAndOwnerId(String name, UUID userId);

    @Modifying
    @Query("UPDATE Workspace w SET w.name = :name WHERE w.id = :workspaceId")
    void updateWorkspaceName(UUID workspaceId, String name);

    Page<Workspace> findAllByIdIn(List<UUID> ids, Pageable pageable);
}
