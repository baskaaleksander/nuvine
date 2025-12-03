CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PAID',
    'FAILED',
    'CANCELED',
    'REFUNDED',
    'PARTIALLY_REFUNDED',
    'VOID'
);

CREATE TABLE payments (
                          id UUID PRIMARY KEY,

                          workspace_id UUID NOT NULL,
                          subscription_id UUID NOT NULL,

                          stripe_invoice_id VARCHAR(128) UNIQUE,
                          stripe_payment_intent_id VARCHAR(128),

                          amount_due NUMERIC(18,2),
                          amount_paid NUMERIC(18,2),

                          currency VARCHAR(8) NOT NULL,

                          status payment_status NOT NULL,

                          billing_period_start TIMESTAMPTZ,
                          billing_period_end TIMESTAMPTZ,

                          invoice_pdf_url VARCHAR(2048),
                          description VARCHAR(512),

                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,

                          metadata_json JSONB
);

CREATE INDEX idx_payments_workspace
    ON payments (workspace_id);

CREATE INDEX idx_payments_subscription
    ON payments (subscription_id);

CREATE UNIQUE INDEX idx_payments_stripe_invoice
    ON payments (stripe_invoice_id);

CREATE INDEX idx_payments_status
    ON payments (status);