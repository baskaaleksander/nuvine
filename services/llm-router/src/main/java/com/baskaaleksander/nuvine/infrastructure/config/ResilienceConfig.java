package com.baskaaleksander.nuvine.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry)
                .bindTo(meterRegistry);

        log.info("CircuitBreakerRegistry initialized with Micrometer metrics");
        return registry;
    }

    @Bean
    public CircuitBreaker openaiEmbeddingsCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig embeddingsConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build();

        CircuitBreaker cb = registry.circuitBreaker("openai-embeddings-global", embeddingsConfig);

        cb.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("OpenAI Embeddings circuit breaker state transition: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onFailureRateExceeded(event ->
                        log.error("OpenAI Embeddings circuit breaker failure rate exceeded: {}%",
                                event.getFailureRate()))
                .onCallNotPermitted(event ->
                        log.warn("OpenAI Embeddings circuit breaker call not permitted"));

        log.info("OpenAI Embeddings global circuit breaker initialized");
        return cb;
    }
}
