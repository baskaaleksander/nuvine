CREATE TABLE workspace_members
(
    id           UUID,
    workspace_id UUID                        NOT NULL,
    user_id      UUID                        NOT NULL,
    role         VARCHAR(255)                NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version      BIGINT                      NOT NULL,
    CONSTRAINT pk_workspace_members primary key (id),
    CONSTRAINT workspace_members_workspace_id_user_id_key unique (workspace_id, user_id)
);