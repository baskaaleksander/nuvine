BEGIN;

CREATE TABLE subscriptions_temp AS
SELECT * FROM subscriptions;

DROP TABLE subscriptions CASCADE;

CREATE TABLE subscriptions (
                               id UUID PRIMARY KEY,
                               workspace_id UUID NOT NULL,
                               plan_id UUID NOT NULL,
                               stripe_customer_id VARCHAR(128) NOT NULL,
                               stripe_subscription_id VARCHAR(128) NOT NULL,
                               status VARCHAR(32) NOT NULL,
                               current_period_start TIMESTAMPTZ NOT NULL,
                               current_period_end TIMESTAMPTZ NOT NULL,
                               cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMPTZ NOT NULL,
                               updated_at TIMESTAMPTZ NOT NULL,
                               CONSTRAINT uq_subscriptions_stripe_subscription_id UNIQUE (stripe_subscription_id)
);

CREATE INDEX idx_subscriptions_workspace_id ON subscriptions(workspace_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_stripe_customer_id ON subscriptions(stripe_customer_id);

INSERT INTO subscriptions (
    id,
    workspace_id,
    plan_id,
    stripe_customer_id,
    stripe_subscription_id,
    status,
    current_period_start,
    current_period_end,
    cancel_at_period_end,
    created_at,
    updated_at
)
SELECT
    id,
    workspace_id,
    plan_id,
    stripe_customer_id,
    stripe_subscription_id,
    status,
    current_period_start,
    current_period_end,
    cancel_at_period_end,
    created_at,
    updated_at
FROM subscriptions_temp;

ALTER TABLE subscriptions
    ADD CONSTRAINT chk_subscription_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'TRIALING',
                       'PAST_DUE',
                       'CANCELED',
                       'INCOMPLETE',
                       'INCOMPLETE_EXPIRED',
                       'UNPAID'
                )
            );

DROP TABLE subscriptions_temp;

COMMIT;