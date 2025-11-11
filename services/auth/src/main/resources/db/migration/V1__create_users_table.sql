CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       first_name VARCHAR(100),
                       last_name VARCHAR(100),
                       onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
                       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX idx_users_email ON users (email);