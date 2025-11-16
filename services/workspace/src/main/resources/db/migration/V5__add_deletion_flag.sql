-- Documents: add deleted flag
ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE documents
SET deleted = FALSE
WHERE deleted IS NULL;

-- Projects: add deleted flag
ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE projects
SET deleted = FALSE
WHERE deleted IS NULL;

-- Workspaces: add deleted flag
ALTER TABLE workspaces
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE workspaces
SET deleted = FALSE
WHERE deleted IS NULL;

-- Workspace Members: add deleted flag
ALTER TABLE workspace_members
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE workspace_members
SET deleted = FALSE
WHERE deleted IS NULL;