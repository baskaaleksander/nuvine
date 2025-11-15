CREATE TABLE workspaces
(
    id              UUID         NOT NULL,
    name            VARCHAR(255) NOT NULL,
    owner_user_id   UUID         NOT NULL,
    billing_tier    VARCHAR(255) NOT NULL DEFAULT 'FREE',
    subscription_id VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    version         BIGINT,
    CONSTRAINT pk_workspaces PRIMARY KEY (id)
);