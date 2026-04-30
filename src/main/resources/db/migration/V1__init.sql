CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    locked      BOOLEAN      NOT NULL DEFAULT FALSE,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    mfa_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_secret  VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE TABLE email_verification_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expiry_date TIMESTAMP    NOT NULL,
    confirmed   BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expiry_date TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE login_attempts (
    id                BIGSERIAL PRIMARY KEY,
    identifier        VARCHAR(255) NOT NULL UNIQUE,
    failed_attempts   INT          NOT NULL DEFAULT 0,
    last_attempt_time TIMESTAMP,
    locked_until      TIMESTAMP
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_evt_token ON email_verification_tokens (token);
CREATE INDEX idx_evt_user ON email_verification_tokens (user_id);
CREATE INDEX idx_rt_token ON refresh_tokens (token);
CREATE INDEX idx_rt_user ON refresh_tokens (user_id);
CREATE INDEX idx_la_identifier ON login_attempts (identifier);
