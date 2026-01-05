package com.baskaaleksander.nuvine.domain.exception;

public class ModelCircuitBreakerOpenException extends CircuitBreakerOpenException {

    private final String modelName;

    public ModelCircuitBreakerOpenException(String modelName) {
        super("openrouter-model-" + sanitize(modelName), "OpenRouter");
        this.modelName = modelName;
    }

    public ModelCircuitBreakerOpenException(String modelName, Throwable cause) {
        super("openrouter-model-" + sanitize(modelName), "OpenRouter", cause);
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    @Override
    public String getMessage() {
        return String.format(
                "Circuit breaker is OPEN for model '%s'. The model has experienced too many failures. Please try again later or use a different model.",
                modelName
        );
    }

    private static String sanitize(String modelName) {
        return modelName.replaceAll("[^a-zA-Z0-9-]", "_");
    }
}
