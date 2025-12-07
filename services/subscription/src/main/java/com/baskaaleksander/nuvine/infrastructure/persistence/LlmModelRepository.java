package com.baskaaleksander.nuvine.infrastructure.persistence;

import com.baskaaleksander.nuvine.domain.model.LlmModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LlmModelRepository extends JpaRepository<LlmModel, UUID> {

    @Query("""
            SELECT m FROM LlmModel m
            JOIN FETCH m.provider p
            WHERE p.providerKey = :provider
            AND m.modelKey = :model
            AND m.active = true
            AND m.effectiveFrom <= :now
            AND (m.effectiveTo IS NULL OR m.effectiveTo > :now)
            """)
    Optional<LlmModel> findActiveModel(
            @Param("provider") String providerKey,
            @Param("model") String modelKey,
            @Param("now") Instant now
    );

    @Query("""
            SELECT m FROM LlmModel m 
            JOIN FETCH m.provider p
            WHERE m.active = true
            AND m.effectiveFrom <= :now
            AND (m.effectiveTo IS NULL OR m.effectiveTo > :now)
            """)
    List<LlmModel> findAllActiveModels(@Param("now") Instant now);
}
