CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(34) NOT NULL UNIQUE,
    balance NUMERIC(19, 2) NOT NULL CHECK (balance >= 0),
    currency VARCHAR(3) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE transfer_transactions (
    id UUID PRIMARY KEY,
    reference VARCHAR(36) NOT NULL UNIQUE,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    request_fingerprint VARCHAR(64) NOT NULL,
    source_account_number VARCHAR(34) NOT NULL,
    destination_account_number VARCHAR(34) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    transaction_fee NUMERIC(19, 2) NOT NULL CHECK (transaction_fee >= 0),
    billed_amount NUMERIC(19, 2) NOT NULL CHECK (billed_amount > 0),
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_message VARCHAR(255) NOT NULL,
    commission_worthy BOOLEAN,
    commission NUMERIC(19, 2),
    commission_processed_at TIMESTAMPTZ,
    CONSTRAINT ck_transfer_status CHECK (
        status IN ('PROCESSING', 'SUCCESSFUL', 'INSUFFICIENT_FUNDS', 'FAILED')
    )
);

CREATE INDEX idx_transfer_created_at ON transfer_transactions (created_at DESC);
CREATE INDEX idx_transfer_status_created_at ON transfer_transactions (status, created_at DESC);
CREATE INDEX idx_transfer_source_account ON transfer_transactions (source_account_number, created_at DESC);
CREATE INDEX idx_transfer_destination_account ON transfer_transactions (destination_account_number, created_at DESC);
CREATE INDEX idx_transfer_commission_pending
    ON transfer_transactions (created_at)
    WHERE commission_processed_at IS NULL AND status <> 'PROCESSING';

CREATE TABLE daily_transaction_summaries (
    summary_date DATE PRIMARY KEY,
    total_count BIGINT NOT NULL,
    successful_count BIGINT NOT NULL,
    insufficient_funds_count BIGINT NOT NULL,
    failed_count BIGINT NOT NULL,
    total_amount NUMERIC(21, 2) NOT NULL,
    total_fees NUMERIC(21, 2) NOT NULL,
    total_billed_amount NUMERIC(21, 2) NOT NULL,
    total_commission NUMERIC(21, 2) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
