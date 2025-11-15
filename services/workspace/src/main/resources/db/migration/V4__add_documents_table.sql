CREATE TABLE documents
(
    id           UUID,
    project_id   UUID                        NOT NULL,
    workspace_id UUID                        NOT NULL,
    name         VARCHAR(255)                NOT NULL,
    status       VARCHAR(255)                NOT NULL,
    storage_key  VARCHAR(255),
    mime_type    VARCHAR(255),
    size_bytes   BIGINT                      NOT NULL,
    created_by   UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version      BIGINT                      NOT NULL,
    CONSTRAINT pk_documents primary key (id)
);