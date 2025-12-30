package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.Plan;
import com.baskaaleksander.nuvine.infrastructure.persistence.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    @Cacheable(cacheNames = "plans", key = "#planId")
    public Optional<Plan> findById(UUID planId) {
        return planRepository.findById(planId);
    }
}
