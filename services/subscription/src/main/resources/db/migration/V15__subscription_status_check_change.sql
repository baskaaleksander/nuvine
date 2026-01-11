ALTER TABLE subscriptions
DROP CONSTRAINT chk_subscription_status;

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
                       'UNPAID',
                       'DELETED'
                )
            );