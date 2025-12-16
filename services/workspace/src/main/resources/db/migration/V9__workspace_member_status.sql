ALTER TABLE workspace_members
    ADD COLUMN status VARCHAR(255) DEFAULT 'PENDING' NOT NULL;

UPDATE workspace_members
SET status = 'PENDING';
