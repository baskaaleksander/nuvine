package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IngestionJobRepository extends JpaRepository<IngestionJob, UUID> {
}
