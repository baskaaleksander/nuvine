ALTER TABLE payments
    ALTER COLUMN status DROP DEFAULT,
ALTER COLUMN status TYPE VARCHAR(32)
    USING status::text;

DROP TYPE payment_status;

ALTER TABLE payments
    ADD CONSTRAINT payments_status_check
        CHECK (
            status IN (
                       'PENDING',
                       'PROCESSING',
                       'SUCCEEDED',
                       'FAILED',
                       'REFUNDED',
                       'CANCELED',
                       'REQUIRES_ACTION'
                )
            );