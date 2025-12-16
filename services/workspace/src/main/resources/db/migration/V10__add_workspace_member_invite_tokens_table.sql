CREATE TABLE workspace_member_invite_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(2048) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used_at TIMESTAMP WITHOUT TIME ZONE,
    workspace_member_id UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT,
    CONSTRAINT uk_workspace_member_invite_tokens_token UNIQUE (token),
    CONSTRAINT fk_workspace_member_invite_tokens_workspace_member
        FOREIGN KEY (workspace_member_id) REFERENCES workspace_members(id) ON DELETE CASCADE
);

CREATE INDEX idx_workspace_member_invite_tokens_workspace_member_id
    ON workspace_member_invite_tokens(workspace_member_id);
