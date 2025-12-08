package com.baskaaleksander.nuvine.infrastructure.persistence;

import com.baskaaleksander.nuvine.domain.model.UsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, UUID> {

    @Query("""
            SELECT u FROM UsageLog u
            WHERE u.workspaceId = :workspaceId
            AND u.occurredAt >= :startDate
            AND u.occurredAt <= :endDate
            """)
    Page<UsageLog> findByWorkspaceIdAndDateRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query("""
            SELECT u FROM UsageLog u
            WHERE u.workspaceId = :workspaceId
            AND u.occurredAt >= :startDate
            AND u.occurredAt <= :endDate
            AND u.provider = :provider
            """)
    Page<UsageLog> findByWorkspaceIdAndDateRangeAndProvider(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("provider") String provider,
            Pageable pageable
    );

    @Query("""
            SELECT u FROM UsageLog u
            WHERE u.workspaceId = :workspaceId
            AND u.occurredAt >= :startDate
            AND u.occurredAt <= :endDate
            AND u.model = :model
            """)
    Page<UsageLog> findByWorkspaceIdAndDateRangeAndModel(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("model") String model,
            Pageable pageable
    );

    @Query("""
            SELECT u FROM UsageLog u
            WHERE u.workspaceId = :workspaceId
            AND u.occurredAt >= :startDate
            AND u.occurredAt <= :endDate
            AND u.provider = :provider
            AND u.model = :model
            """)
    Page<UsageLog> findByWorkspaceIdAndDateRangeAndProviderAndModel(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("provider") String provider,
            @Param("model") String model,
            Pageable pageable
    );

    @Query("""
            SELECT u FROM UsageLog u
            WHERE u.workspaceId = :workspaceId
            AND u.occurredAt >= :startDate
            AND u.occurredAt <= :endDate
            """)
    List<UsageLog> findByWorkspaceIdAndDateRangeList(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
