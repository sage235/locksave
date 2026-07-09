-- V1__create_users_table.sql

CREATE TABLE users (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name           VARCHAR(100)    NOT NULL,
    email               VARCHAR(150)    NOT NULL UNIQUE,
    password_hash       VARCHAR(255)    NOT NULL,
    phone_number        VARCHAR(20)     NOT NULL UNIQUE,
    is_email_verified   BOOLEAN         NOT NULL DEFAULT FALSE,
    is_phone_verified   BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    role                VARCHAR(20)     NOT NULL DEFAULT 'USER',
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email        ON users(email);
CREATE INDEX idx_users_phone        ON users(phone_number);