CREATE TABLE password_reset_tokens (
                                           id UUID PRIMARY KEY,
                                           token VARCHAR(2048) NOT NULL,
                                           expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                                           used_at TIMESTAMP WITHOUT TIME ZONE,
                                           user_id UUID NOT NULL,
                                           created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                                           updated_at TIMESTAMP WITHOUT TIME ZONE,
                                           version BIGINT,
                                           CONSTRAINT uk_password_reset_tokens_token UNIQUE (token),
                                           CONSTRAINT fk_password_reset_tokens_user
                                               FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON password_reset_tokens(user_id);