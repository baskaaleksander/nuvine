ALTER TABLE conversation_message
    ADD COLUMN owner_id UUID NOT NULL;

ALTER TABLE conversation_message
    ALTER COLUMN tokens_cost DROP NOT NULL;