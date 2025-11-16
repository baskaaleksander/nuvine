package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("select count(*) from Project p where p.workspaceId = :workspaceId")
    Long getProjectCountByWorkspaceId(UUID workspaceId);

    @Query("select count(*) > 0 from Project p where p.name = :name and p.workspaceId = :workspaceId and p.deleted = false")
    boolean existsByNameAndWorkspaceId(String name, UUID workspaceId);

    @Query("select p from Project p where p.workspaceId = :workspaceId and p.deleted = false")
    Page<Project> findAllByWorkspaceId(UUID workspaceId, Pageable pageable);

}
