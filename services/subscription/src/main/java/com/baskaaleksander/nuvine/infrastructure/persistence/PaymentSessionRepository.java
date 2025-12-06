package com.baskaaleksander.nuvine.infrastructure.persistence;

import com.baskaaleksander.nuvine.domain.model.PaymentSession;
import com.baskaaleksander.nuvine.domain.model.PaymentSessionIntent;
import com.baskaaleksander.nuvine.domain.model.PaymentSessionStatus;
import com.baskaaleksander.nuvine.domain.model.PaymentSessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentSessionRepository extends JpaRepository<PaymentSession, UUID> {

    @Query("""
                SELECT ps
                FROM PaymentSession ps
                WHERE ps.workspaceId = :workspaceId
                AND ps.planId = :planId
                AND ps.type = :type
                AND ps.intent = :intent
                AND ps.userId = :userId
                AND ps.expiresAt > :now
                AND ps.status = :status
            """)
    Optional<PaymentSession> findValidSession(UUID workspaceId, UUID planId, PaymentSessionType type, PaymentSessionIntent intent, UUID userId, Instant now, PaymentSessionStatus status);

    Optional<PaymentSession> findByStripeSessionId(String stripeSessionId);
}
