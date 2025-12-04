package com.baskaaleksander.nuvine.infrastructure.persistence;

import com.baskaaleksander.nuvine.domain.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
}
