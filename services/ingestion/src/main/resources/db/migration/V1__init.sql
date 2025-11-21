CREATE TABLE ingestion_jobs
(
    id           UUID PRIMARY KEY,
    document_id  UUID                        NOT NULL,
    workspace_id UUID                        NOT NULL,
    project_id   UUID                        NOT NULL,
    storage_key  VARCHAR(512)                NOT NULL,
    mime_type    VARCHAR(255)                NOT NULL,
    status       VARCHAR(50)                 NOT NULL,
    stage        VARCHAR(50)                 NOT NULL,
    retry_count  INT                         NOT NULL,
    last_error   TEXT,
    created_by   UUID,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version      BIGINT
);

CREATE INDEX idx_ingestion_jobs_document_id
    ON ingestion_jobs (document_id);

CREATE INDEX idx_ingestion_jobs_workspace_project
    ON ingestion_jobs (workspace_id, project_id);

CREATE INDEX idx_ingestion_jobs_status_stage_created_at
    ON ingestion_jobs (status, stage, created_at);

CREATE INDEX idx_ingestion_jobs_created_at
    ON ingestion_jobs (created_at);