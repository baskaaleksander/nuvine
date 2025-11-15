CREATE TABLE projects
(
    id           UUID                        NOT NULL,
    name         VARCHAR(255)                NOT NULL,
    description  VARCHAR(255),
    workspace_id UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version      BIGINT                      NOT NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id)
);