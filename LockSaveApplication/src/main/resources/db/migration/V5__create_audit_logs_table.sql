-- V4__create_notifications_table.sql

CREATE TYPE notification_type AS ENUM (
    'DEPOSIT_CONFIRMED',
    'WITHDRAWAL_COMPLETED',
    'VAULT_UNLOCKED',
    'GOAL_REACHED',
    'OTP',
    'ACCOUNT_ALERT'
);

CREATE TYPE notification_channel AS ENUM (
    'EMAIL',
    'SMS',
    'IN_APP'
);

CREATE TYPE notification_status AS ENUM (
    'PENDING',
    'SENT',
    'FAILED'
);

CREATE TABLE notifications (
    id              UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID                    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            notification_type       NOT NULL,
    channel         notification_channel    NOT NULL,
    title           VARCHAR(150)            NOT NULL,
    message         TEXT                    NOT NULL,
    status          notification_status     NOT NULL DEFAULT 'PENDING',
    is_read         BOOLEAN                 NOT NULL DEFAULT FALSE,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP               NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status  ON notifications(status);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);