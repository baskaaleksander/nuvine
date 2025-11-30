ALTER TABLE conversation_message
    ADD COLUMN project_id UUID NOT NULL;
ALTER TABLE conversation_message
    ADD COLUMN workspace_id UUID NOT NULL;