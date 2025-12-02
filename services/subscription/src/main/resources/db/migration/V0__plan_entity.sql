-- Create enums
CREATE TYPE billing_period AS ENUM ('MONTHLY', 'YEARLY');
CREATE TYPE hard_limit_behaviour AS ENUM ('BLOCK', 'SOFT_BLOCK', 'ALLOW_OVERAGE');

-- Create table
CREATE TABLE plans (
                       id UUID PRIMARY KEY,
                       code VARCHAR(64) NOT NULL,
                       name VARCHAR(128) NOT NULL,
                       stripe_price_id VARCHAR(128) NOT NULL,
                       billing_period billing_period NOT NULL,
                       included_credits VARCHAR(64) NOT NULL,
                       max_storage_size BIGINT NOT NULL,
                       hard_limit_behaviour hard_limit_behaviour NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Unique constraint
ALTER TABLE plans
    ADD CONSTRAINT uq_plans_code UNIQUE (code);

-- Indexes
CREATE INDEX idx_plans_code ON plans (code);
CREATE INDEX idx_plans_stripe_price_id ON plans (stripe_price_id);
CREATE INDEX idx_plans_billing_period ON plans (billing_period);