package com.baskaaleksander.nuvine.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenRouterCircuitBreakerRegistryTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private OpenRouterCircuitBreakerRegistry registry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        registry = new OpenRouterCircuitBreakerRegistry(circuitBreakerRegistry);
        registry.init();
    }

    @Test
    void getCircuitBreaker_createsNewCircuitBreakerForModel() {
        CircuitBreaker cb = registry.getCircuitBreaker("gpt-4");

        assertNotNull(cb);
        assertTrue(cb.getName().contains("gpt-4"));
    }

    @Test
    void getCircuitBreaker_returnsSameInstanceForSameModel() {
        CircuitBreaker cb1 = registry.getCircuitBreaker("gpt-4");
        CircuitBreaker cb2 = registry.getCircuitBreaker("gpt-4");

        assertSame(cb1, cb2);
    }

    @Test
    void getCircuitBreaker_createsDifferentInstancesForDifferentModels() {
        CircuitBreaker cb1 = registry.getCircuitBreaker("gpt-4");
        CircuitBreaker cb2 = registry.getCircuitBreaker("claude-3-opus");

        assertNotSame(cb1, cb2);
        assertTrue(cb1.getName().contains("gpt-4"));
        assertTrue(cb2.getName().contains("claude-3-opus"));
    }

    @Test
    void getCircuitBreaker_sanitizesModelName() {
        CircuitBreaker cb = registry.getCircuitBreaker("openai/gpt-4-turbo");

        assertNotNull(cb);
        assertTrue(cb.getName().contains("openai_gpt-4-turbo"));
    }

    @Test
    void isOpen_returnsFalseForClosedCircuitBreaker() {
        registry.getCircuitBreaker("gpt-4");

        assertFalse(registry.isOpen("gpt-4"));
    }

    @Test
    void isOpen_returnsTrueForOpenCircuitBreaker() {
        CircuitBreaker cb = registry.getCircuitBreaker("gpt-4");
        cb.transitionToOpenState();

        assertTrue(registry.isOpen("gpt-4"));
    }

    @Test
    void getCircuitBreaker_initialStateIsClosed() {
        CircuitBreaker cb = registry.getCircuitBreaker("gpt-4");

        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }
}
