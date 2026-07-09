-- V5__create_audit_logs_table.sql

CREATE TYPE audit_action AS ENUM (
    'USER_REGISTERED',
    'USER_LOGIN',
    'USER_LOGIN_FAILED',
    'OTP_REQUESTED',
    'OTP_VERIFIED',
    'VAULT_CREATED',
    'VAULT_CLOSED',
    'DEPOSIT_INITIATED',
    'DEPOSIT_CONFIRMED',
    'WITHDRAWAL_INITIATED',
    'WITHDRAWAL_COMPLETED',
    'PROFILE_UPDATED',
    'PASSWORD_CHANGED'
);

CREATE TABLE audit_logs (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            REFERENCES users(id) ON DELETE SET NULL,
    action          audit_action    NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       UUID,
    description     TEXT,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_user_id    ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action     ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_entity     ON audit_logs(entity_type, entity_id);