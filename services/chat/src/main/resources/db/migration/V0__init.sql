CREATE TABLE IF NOT EXISTS conversation_message (
                                                    id              UUID PRIMARY KEY,
                                                    conversation_id UUID        NOT NULL,
                                                    content         TEXT        NOT NULL,
                                                    role            VARCHAR(32) NOT NULL,
    model_used      VARCHAR(100) NOT NULL,
    tokens_in       INTEGER     NOT NULL DEFAULT 0,
    tokens_out      INTEGER     NOT NULL DEFAULT 0,
    cost            DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_conversation_message_conversation_created_at
    ON conversation_message (conversation_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_conversation_message_created_at
    ON conversation_message (created_at);