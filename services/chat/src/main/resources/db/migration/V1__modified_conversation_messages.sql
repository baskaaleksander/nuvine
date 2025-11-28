ALTER TABLE conversation_message
DROP COLUMN IF EXISTS tokens_in;

ALTER TABLE conversation_message
DROP COLUMN IF EXISTS tokens_out;

ALTER TABLE conversation_message
    ADD COLUMN tokens_cost INTEGER NOT NULL DEFAULT 0;