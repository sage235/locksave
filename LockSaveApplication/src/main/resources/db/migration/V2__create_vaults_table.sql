-- V2__create_vaults_table.sql

CREATE TYPE vault_status AS ENUM (
    'ACTIVE',
    'UNLOCKED',
    'CLOSED'
);

CREATE TABLE vaults (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title           VARCHAR(100)    NOT NULL,
    description     VARCHAR(255),
    goal_amount     NUMERIC(15, 2)  NOT NULL CHECK (goal_amount > 0),
    current_balance NUMERIC(15, 2)  NOT NULL DEFAULT 0.00 CHECK (current_balance >= 0),
    unlock_date     DATE            NOT NULL,
    status          vault_status    NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now(),

    CONSTRAINT chk_unlock_date_future CHECK (unlock_date > CURRENT_DATE)
);

CREATE INDEX idx_vaults_user_id ON vaults(user_id);
CREATE INDEX idx_vaults_status  ON vaults(status);