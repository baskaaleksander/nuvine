CREATE TYPE usage_metric AS ENUM (
    'CREDITS',
    'TOKENS',
    'REQUESTS',
    'STORAGE'
);

CREATE TABLE subscription_usage_counters (
                                             id UUID PRIMARY KEY,

                                             subscription_id UUID NOT NULL,

                                             period_start DATE NOT NULL,
                                             period_end DATE NOT NULL,

                                             metric usage_metric NOT NULL,

                                             used_value NUMERIC(18,6) NOT NULL,

                                             created_at TIMESTAMPTZ NOT NULL,
                                             updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_sub_usage_unique
    ON subscription_usage_counters (subscription_id, period_start, period_end, metric);