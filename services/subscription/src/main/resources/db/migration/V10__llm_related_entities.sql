CREATE TABLE llm_providers
(
    id           UUID PRIMARY KEY,
    provider_key VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    active       BOOLEAN      NOT NULL,
    CONSTRAINT pk_llm_providers PRIMARY KEY (id)
);

CREATE TABLE llm_models
(
    id                              UUID PRIMARY KEY,
    provider_id                     UUID           NOT NULL,
    model_key                       VARCHAR(255)   NOT NULL,
    display_name                    VARCHAR(255),
    max_output_tokens               BIGINT         NOT NULL,

    input_price_per1m_tokens        DECIMAL(12, 8) NOT NULL,
    output_price_per1m_tokens       DECIMAL(12, 8) NOT NULL,
    image_price_per1k               DECIMAL(12, 8),
    audio_input_price_per1m_tokens  DECIMAL(12, 8),
    audio_output_price_per1m_tokens DECIMAL(12, 8),
    currency                        VARCHAR(3)     NOT NULL DEFAULT 'USD',

    free                            BOOLEAN        NOT NULL DEFAULT FALSE,
    active                          BOOLEAN        NOT NULL,
    effective_from                  TIMESTAMP      NOT NULL,
    effective_to                    TIMESTAMP,

    CONSTRAINT pk_llm_models PRIMARY KEY (id),
    CONSTRAINT fk_llm_models_provider FOREIGN KEY (provider_id)
        REFERENCES llm_providers (id) ON DELETE CASCADE,
    CONSTRAINT uq_provider_model UNIQUE (provider_id, model_key)
);

CREATE INDEX idx_llm_models_provider_id ON llm_models (provider_id);
CREATE INDEX idx_llm_models_active ON llm_models (active);
CREATE INDEX idx_llm_models_effective_dates ON llm_models (effective_from, effective_to);
CREATE INDEX idx_llm_providers_active ON llm_providers (active);