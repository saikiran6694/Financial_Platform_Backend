CREATE TABLE users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    password         VARCHAR(255) NOT NULL,
    profile_picture  VARCHAR(1024),
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX idx_users_email ON users (email);

CREATE TABLE transactions (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL REFERENCES users (id),
    title                 VARCHAR(255),
    type                  VARCHAR(20) NOT NULL,
    amount                BIGINT NOT NULL,
    category              VARCHAR(255),
    receipt_url           VARCHAR(1024),
    recurring_interval    VARCHAR(20),
    next_recurring_date   TIMESTAMPTZ,
    last_processed        TIMESTAMPTZ,
    is_recurring          BOOLEAN NOT NULL DEFAULT FALSE,
    description           TEXT,
    date                  TIMESTAMPTZ NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    payment_method        VARCHAR(20) NOT NULL DEFAULT 'CASH',
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_transactions_user_date ON transactions (user_id, date);
CREATE INDEX idx_transactions_user_category ON transactions (user_id, category);

CREATE TABLE budgets (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL REFERENCES users (id),
    category              VARCHAR(255) NOT NULL,
    limit_amount          BIGINT NOT NULL,
    period                VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    alert_threshold       DOUBLE PRECISION NOT NULL DEFAULT 0.8,
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    last_alerted_period   VARCHAR(20),
    last_alerted_level    VARCHAR(20),
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_budgets_user_category ON budgets (user_id, category);

CREATE TABLE reports (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users (id),
    period       VARCHAR(255),
    sent_date    TIMESTAMPTZ,
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_reports_user_created ON reports (user_id, created_at);

CREATE TABLE report_settings (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users (id),
    frequency          VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    is_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    next_report_date   TIMESTAMPTZ,
    last_sent_date     TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX idx_report_settings_user ON report_settings (user_id);
CREATE INDEX idx_report_settings_next_date ON report_settings (next_report_date);

CREATE TABLE scheduler (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users (id),
    timezone         VARCHAR(255) NOT NULL,
    scheduled_time   VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX idx_scheduler_user ON scheduler (user_id);

CREATE TABLE forgot_password (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL,
    otp          VARCHAR(20) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    verified     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_forgot_password_email_otp ON forgot_password (email, otp);
CREATE INDEX idx_forgot_password_expires_at ON forgot_password (expires_at);
