CREATE TABLE payments (
 id UUID PRIMARY KEY,
 merchant_id VARCHAR(100) NOT NULL,
 idempotency_key VARCHAR(160) NOT NULL,
 request_fingerprint VARCHAR(64) NOT NULL,
 amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
 currency CHAR(3) NOT NULL,
 payment_method VARCHAR(30) NOT NULL,
 status VARCHAR(20) NOT NULL,
 provider_reference VARCHAR(120),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (merchant_id, idempotency_key)
);
CREATE TABLE provider_callbacks (
 provider_event_id VARCHAR(160) PRIMARY KEY,
 payment_id UUID NOT NULL REFERENCES payments(id),
 received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE ledger_transactions (
 id UUID PRIMARY KEY,
 payment_id UUID NOT NULL REFERENCES payments(id),
 transaction_type VARCHAR(30) NOT NULL,
 currency CHAR(3) NOT NULL
);
CREATE TABLE ledger_entries (
 id UUID PRIMARY KEY,
 ledger_transaction_id UUID NOT NULL REFERENCES ledger_transactions(id),
 account_code VARCHAR(80) NOT NULL,
 entry_side VARCHAR(6) NOT NULL CHECK (entry_side IN ('DEBIT','CREDIT')),
 amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
 currency CHAR(3) NOT NULL
);
CREATE TABLE outbox_events (
 id UUID PRIMARY KEY,
 aggregate_id UUID NOT NULL,
 event_type VARCHAR(100) NOT NULL,
 payload TEXT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_pending ON outbox_events(created_at) WHERE published_at IS NULL;
