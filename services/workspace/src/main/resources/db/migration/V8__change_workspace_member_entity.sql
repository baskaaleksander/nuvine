ALTER TABLE workspace_members
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE workspace_members
    ADD COLUMN email VARCHAR(255) DEFAULT 'placeholder@example.com';

UPDATE workspace_members
SET email = 'placeholder@example.com'
WHERE email IS NULL;

ALTER TABLE workspace_members
    ALTER COLUMN email SET NOT NULL;

ALTER TABLE workspace_members
    ADD COLUMN user_name VARCHAR(255);