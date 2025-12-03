CREATE TABLE payment_sessions
(
    id                UUID PRIMARY KEY,

    workspace_id      UUID          NOT NULL,
    user_id           UUID          NOT NULL,

    type              VARCHAR(32)   NOT NULL,
    intent            VARCHAR(64)   NOT NULL,

    stripe_session_id VARCHAR(128)  NOT NULL UNIQUE,
    stripe_url        VARCHAR(2048) NOT NULL,

    status            VARCHAR(32)   NOT NULL,

    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    expires_at        TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,

    metadata_json     JSONB
);

CREATE INDEX idx_payment_sessions_workspace
    ON payment_sessions (workspace_id);

CREATE INDEX idx_payment_sessions_stripe_session
    ON payment_sessions (stripe_session_id);

CREATE INDEX idx_payment_sessions_status
    ON payment_sessions (status);