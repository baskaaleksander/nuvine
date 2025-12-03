CREATE TABLE usage_logs
(
    id              UUID PRIMARY KEY,

    subscription_id UUID           NOT NULL,
    workspace_id    UUID           NOT NULL,
    user_id         UUID,
    conversation_id UUID,
    message_id      UUID,

    source_service  VARCHAR(64)    NOT NULL,
    provider        VARCHAR(64),
    model           VARCHAR(128)   NOT NULL,

    tokens_in       BIGINT         NOT NULL,
    tokens_out      BIGINT         NOT NULL,

    cost_credits    NUMERIC(18, 6) NOT NULL,

    created_at      TIMESTAMPTZ    NOT NULL,
    occurred_at     TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_usage_logs_subscription_period
    ON usage_logs (subscription_id, occurred_at);

CREATE INDEX idx_usage_logs_workspace_period
    ON usage_logs (workspace_id, occurred_at);