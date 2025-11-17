package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("select count(*) from Document d where d.projectId = :projectId")
    Long getDocumentCountByProjectId(UUID projectId);

    Page<Document> findAllByProjectId(UUID projectId, Pageable pageable);
}
