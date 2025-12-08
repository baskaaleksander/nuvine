package com.baskaaleksander.nuvine.infrastructure.persistence;

import com.baskaaleksander.nuvine.domain.model.Payment;
import com.baskaaleksander.nuvine.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("""
            SELECT p FROM Payment p
            WHERE p.workspaceId = :workspaceId
            AND p.createdAt >= :startDate
            AND p.createdAt <= :endDate
            """)
    Page<Payment> findByWorkspaceIdAndDateRange(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query("""
            SELECT p FROM Payment p
            WHERE p.workspaceId = :workspaceId
            AND p.createdAt >= :startDate
            AND p.createdAt <= :endDate
            AND p.status = :status
            """)
    Page<Payment> findByWorkspaceIdAndDateRangeAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("status") PaymentStatus status,
            Pageable pageable
    );
}
