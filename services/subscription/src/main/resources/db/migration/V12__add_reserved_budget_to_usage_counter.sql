ALTER TABLE subscription_usage_counters
    ADD COLUMN reserved_budget NUMERIC(18,6) NOT NULL DEFAULT 0;
