-- V3__create_transactions_table.sql

CREATE TYPE transaction_type AS ENUM (
    'DEPOSIT',
    'WITHDRAWAL'
);

CREATE TYPE payment_method AS ENUM (
    'MTN_MOMO',
    'AIRTEL_MONEY',
    'ORANGE_MONEY',
    'VISA',
    'MASTERCARD'
);

CREATE TYPE transaction_status AS ENUM (
    'PENDING',
    'COMPLETED',
    'FAILED',
    'REVERSED'
);

CREATE TABLE transactions (
    id                      UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    vault_id                UUID                NOT NULL REFERENCES vaults(id) ON DELETE RESTRICT,
    transaction_type        transaction_type    NOT NULL,
    amount                  NUMERIC(15, 2)      NOT NULL CHECK (amount > 0),
    payment_method          payment_method      NOT NULL,
    status                  transaction_status  NOT NULL DEFAULT 'PENDING',
    transaction_reference   VARCHAR(100)        NOT NULL UNIQUE,
    idempotency_key         VARCHAR(100)        NOT NULL UNIQUE,
    provider_reference      VARCHAR(100),
    failure_reason          VARCHAR(255),
    created_at              TIMESTAMP           NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP           NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_vault_id   ON transactions(vault_id);
CREATE INDEX idx_transactions_reference  ON transactions(transaction_reference);
CREATE INDEX idx_transactions_idempotency ON transactions(idempotency_key);
CREATE INDEX idx_transactions_status     ON transactions(status);