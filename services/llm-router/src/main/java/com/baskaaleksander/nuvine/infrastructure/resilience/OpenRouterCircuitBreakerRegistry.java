package com.baskaaleksander.nuvine.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenRouterCircuitBreakerRegistry {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ConcurrentHashMap<String, CircuitBreaker> modelCircuitBreakers = new ConcurrentHashMap<>();

    private CircuitBreakerConfig modelConfig;

    @PostConstruct
    public void init() {
        this.modelConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        log.info("OpenRouterCircuitBreakerRegistry initialized");
    }

    public CircuitBreaker getCircuitBreaker(String modelName) {
        String sanitizedName = sanitizeModelName(modelName);
        String circuitBreakerName = "openrouter-model-" + sanitizedName;

        return modelCircuitBreakers.computeIfAbsent(circuitBreakerName, name -> {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name, modelConfig);

            cb.getEventPublisher()
                    .onStateTransition(event ->
                            log.warn("Circuit breaker '{}' state transition: {} -> {}",
                                    name, event.getStateTransition().getFromState(),
                                    event.getStateTransition().getToState()))
                    .onFailureRateExceeded(event ->
                            log.error("Circuit breaker '{}' failure rate exceeded: {}%",
                                    name, event.getFailureRate()))
                    .onCallNotPermitted(event ->
                            log.warn("Circuit breaker '{}' call not permitted", name));

            log.info("Created new circuit breaker for model: {}", modelName);
            return cb;
        });
    }

    public boolean isOpen(String modelName) {
        CircuitBreaker cb = getCircuitBreaker(modelName);
        return cb.getState() == CircuitBreaker.State.OPEN;
    }

    private String sanitizeModelName(String modelName) {
        return modelName.replaceAll("[^a-zA-Z0-9-]", "_");
    }
}
