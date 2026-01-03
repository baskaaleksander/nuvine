package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("select count(*) from Document d where d.projectId = :projectId")
    Long getDocumentCountByProjectId(UUID projectId);

    @Query("select d from Document d where d.projectId = :projectId and d.deleted = false")
    Page<Document> findAllByProjectId(UUID projectId, Pageable pageable);

    @Query("select d.id from Document d where d.projectId = :projectId and d.deleted = false")
    List<UUID> findDocIdsByProjectId(UUID projectId);

    @Query("""
            SELECT d FROM Document d
            WHERE d.projectId = :projectId
            AND d.deleted = false
            AND (:status IS NULL OR d.status = :status)
            AND (:createdAtFrom IS NULL OR d.createdAt >= :createdAtFrom)
            AND (:createdAtTo IS NULL OR d.createdAt <= :createdAtTo)
            """)
    Page<Document> findAllByProjectIdWithFilters(
            UUID projectId,
            DocumentStatus status,
            Instant createdAtFrom,
            Instant createdAtTo,
            Pageable pageable
    );
}
