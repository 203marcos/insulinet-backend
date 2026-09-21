CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ix_user_account_email ON user_account (email);

ALTER TABLE insulin ADD COLUMN user_id BIGINT REFERENCES user_account (id);
