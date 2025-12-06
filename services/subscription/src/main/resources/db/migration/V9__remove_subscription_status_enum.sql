ALTER TABLE subscriptions
    ADD COLUMN status_temp VARCHAR(32);

UPDATE subscriptions
SET status_temp = status::TEXT;

ALTER TABLE subscriptions
DROP COLUMN status;

DROP INDEX IF EXISTS idx_subscriptions_status;

ALTER TABLE subscriptions
    RENAME COLUMN status_temp TO status;

ALTER TABLE subscriptions
    ALTER COLUMN status SET NOT NULL;

CREATE INDEX idx_subscriptions_status
    ON subscriptions (status);

DROP TYPE IF EXISTS subscription_status CASCADE;

ALTER TABLE subscriptions
    ADD CONSTRAINT chk_subscription_status
        CHECK (status IN (
                          'ACTIVE',
                          'TRIALING',
                          'PAST_DUE',
                          'CANCELED',
                          'INCOMPLETE',
                          'INCOMPLETE_EXPIRED',
                          'UNPAID'
            ));