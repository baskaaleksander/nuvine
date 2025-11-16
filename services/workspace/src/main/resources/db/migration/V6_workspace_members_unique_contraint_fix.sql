ALTER TABLE workspace_members
DROP CONSTRAINT IF EXISTS workspace_members_workspace_id_user_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_workspace_member_active
    ON workspace_members(workspace_id, user_id)
    WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_workspace_owner_per_workspace
    ON workspace_members(workspace_id)
    WHERE role = 'OWNER' AND deleted = false;