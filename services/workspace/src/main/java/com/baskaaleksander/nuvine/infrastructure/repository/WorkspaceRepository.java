package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("SELECT COUNT(w) > 0 FROM Workspace w WHERE w.name = :name AND w.ownerUserId = :userId")
    boolean existsByNameAndOwnerId(String name, UUID userId);

    List<Workspace> findAllByIdIn(List<UUID> ids);
}
