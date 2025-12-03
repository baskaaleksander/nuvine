CREATE TYPE subscription_status AS ENUM (
    'ACTIVE',
    'TRIALING',
    'PAST_DUE',
    'CANCELED',
    'INCOMPLETE',
    'INCOMPLETE_EXPIRED',
    'UNPAID'
);

CREATE TABLE subscriptions (
                               id UUID PRIMARY KEY,
                               workspace_id UUID NOT NULL,
                               plan_id UUID NOT NULL,
                               stripe_customer_id VARCHAR(128) NOT NULL,
                               stripe_subscription_id VARCHAR(128) NOT NULL,
                               status subscription_status NOT NULL,
                               current_period_start TIMESTAMPTZ NOT NULL,
                               current_period_end TIMESTAMPTZ NOT NULL,
                               cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMPTZ NOT NULL,
                               updated_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE subscriptions
    ADD CONSTRAINT uq_subscriptions_stripe_subscription_id
        UNIQUE (stripe_subscription_id);

CREATE INDEX idx_subscriptions_workspace_id
    ON subscriptions (workspace_id);

CREATE INDEX idx_subscriptions_plan_id
    ON subscriptions (plan_id);

CREATE INDEX idx_subscriptions_status
    ON subscriptions (status);

CREATE INDEX idx_subscriptions_stripe_customer_id
    ON subscriptions (stripe_customer_id);

ALTER TABLE subscriptions
    ADD CONSTRAINT fk_subscriptions_plan_id
        FOREIGN KEY (plan_id)
            REFERENCES plans (id);
