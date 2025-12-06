package com.baskaaleksander.nuvine.infrastructure.persistence;

import com.baskaaleksander.nuvine.domain.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByWorkspaceId(UUID workspaceId);

    Subscription findByStripeSubscriptionId(String stripeSubscriptionId);

    List<Subscription> findByStripeCustomerId(String stripeCustomerId);
}
