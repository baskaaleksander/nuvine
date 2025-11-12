CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY,
                                token VARCHAR(512) NOT NULL,
                                expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                used_at TIMESTAMP WITHOUT TIME ZONE,
                                user_id UUID NOT NULL,
                                created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMP WITHOUT TIME ZONE,
                                version BIGINT,
                                CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);

CREATE INDEX idx_refresh_tokens_user_revoked
    ON refresh_tokens(user_id, revoked);