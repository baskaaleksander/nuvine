package com.baskaaleksander.nuvine.domain.exception;

public class CircuitBreakerOpenException extends RuntimeException {

    private final String circuitBreakerName;
    private final String serviceName;

    public CircuitBreakerOpenException(String circuitBreakerName, String serviceName) {
        super(String.format(
                "Circuit breaker '%s' is OPEN for service '%s'. Request rejected.",
                circuitBreakerName,
                serviceName
        ));
        this.circuitBreakerName = circuitBreakerName;
        this.serviceName = serviceName;
    }

    public CircuitBreakerOpenException(String circuitBreakerName, String serviceName, Throwable cause) {
        super(String.format(
                "Circuit breaker '%s' is OPEN for service '%s'. Request rejected.",
                circuitBreakerName,
                serviceName
        ), cause);
        this.circuitBreakerName = circuitBreakerName;
        this.serviceName = serviceName;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
