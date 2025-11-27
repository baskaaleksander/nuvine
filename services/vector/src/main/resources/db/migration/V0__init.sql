CREATE TABLE embedding_jobs
(
    id               UUID PRIMARY KEY,
    workspace_id     UUID        NOT NULL,
    project_id       UUID        NOT NULL,
    document_id      UUID        NOT NULL,
    status           VARCHAR(50) NOT NULL,
    total_chunks     INTEGER     NOT NULL,
    processed_chunks INTEGER     NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ
);

CREATE INDEX idx_embedding_jobs_workspace_project
    ON embedding_jobs (workspace_id, project_id);

CREATE INDEX idx_embedding_jobs_document
    ON embedding_jobs (document_id);

CREATE INDEX idx_embedding_jobs_status
    ON embedding_jobs (status);