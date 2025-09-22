SELECT pg_create_physical_replication_slot('replica_slot');

CREATE TABLE IF NOT EXISTS event_store (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    event_metadata JSONB,
    sequence_number BIGINT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_event_store_aggregate_id_sequence
ON event_store (aggregate_id, sequence_number);

-- Read models for auction-api

-- Denormalized view for auction summary (listing auctions)
CREATE VIEW auction_summary AS
SELECT
    a.id AS auction_id,
    i.title AS item_title,
    i.description AS item_description,
    u.display_name AS seller_name,
    i.category_id AS category,
    a.start_ts,
    a.end_ts,
    a.status,
    a.reserve_price,
    a.buy_now_price,
    -- Assuming current_highest_bid is computed from bids
    (SELECT MAX(amount) FROM bids b WHERE b.auction_id = a.id AND b.accepted_bool = true) AS current_highest_bid,
    (SELECT COUNT(*) FROM bids b WHERE b.auction_id = a.id AND b.accepted_bool = true) AS bid_count,
    a.created_at
FROM auctions a
JOIN items i ON a.item_id = i.id
JOIN sellers s ON i.seller_id = s.id
JOIN users u ON s.user_id = u.id;

-- Auction details table (for detailed auction info)
CREATE TABLE auction_details (
    auction_id VARCHAR(255) PRIMARY KEY,
    item_id VARCHAR(255),
    seller_id VARCHAR(255),
    title VARCHAR(255),
    description TEXT,
    category VARCHAR(255),
    images JSONB,
    metadata JSONB,
    auction_type VARCHAR(50),
    start_ts TIMESTAMP WITH TIME ZONE,
    end_ts TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50),
    reserve_price DECIMAL(10,2),
    buy_now_price DECIMAL(10,2),
    increment_strategy VARCHAR(255),
    extension_policy VARCHAR(255),
    current_highest_bid DECIMAL(10,2),
    highest_bidder_id VARCHAR(255),
    bid_count INT,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_auction_details_status ON auction_details (status);
CREATE INDEX idx_auction_details_end_ts ON auction_details (end_ts);
CREATE INDEX idx_auction_details_category ON auction_details (category);

-- Bid history table (for bid listings)
CREATE TABLE bid_history (
    id BIGSERIAL PRIMARY KEY,
    auction_id VARCHAR(255),
    bidder_id VARCHAR(255),
    amount DECIMAL(10,2),
    server_ts TIMESTAMP WITH TIME ZONE,
    seq_no BIGINT,
    accepted BOOLEAN
);

CREATE INDEX idx_bid_history_auction_id_ts ON bid_history (auction_id, server_ts DESC);
CREATE INDEX idx_bid_history_bidder_id_ts ON bid_history (bidder_id, server_ts DESC);

-- User watchlist table
CREATE TABLE user_watchlist (
    user_id VARCHAR(255),
    auction_id VARCHAR(255),
    added_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (user_id, auction_id)
);

CREATE INDEX idx_user_watchlist_user_id_added ON user_watchlist (user_id, added_at DESC);
CREATE INDEX idx_user_watchlist_auction_id ON user_watchlist (auction_id);

-- Seller dashboard table
CREATE TABLE seller_dashboard (
    seller_id VARCHAR(255),
    auction_id VARCHAR(255),
    title VARCHAR(255),
    status VARCHAR(50),
    end_ts TIMESTAMP WITH TIME ZONE,
    current_highest_bid DECIMAL(10,2),
    bid_count INT,
    total_watchers INT,
    created_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (seller_id, auction_id)
);

CREATE INDEX idx_seller_dashboard_seller_status ON seller_dashboard (seller_id, status);
CREATE INDEX idx_seller_dashboard_seller_end_ts ON seller_dashboard (seller_id, end_ts);

-- Refresh status table for tracking last processed event per projection
CREATE TABLE refresh_status (
    projection_name VARCHAR(255) PRIMARY KEY,
    last_event_id VARCHAR(255),
    last_processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_refresh_status_last_event_id ON refresh_status (last_event_id);

-- Scheduled jobs table for durable timer scheduling
CREATE TABLE scheduled_jobs (
    job_id UUID PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    execute_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    attempts INT NOT NULL DEFAULT 0,
    lease_until TIMESTAMP WITH TIME ZONE,
    leased_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scheduled_jobs_execute_at_status ON scheduled_jobs (execute_at, status);
CREATE INDEX idx_scheduled_jobs_lease_until ON scheduled_jobs (lease_until);
CREATE INDEX idx_scheduled_jobs_auction_id ON scheduled_jobs (auction_id);

-- Escrow transactions table for tracking escrow states
CREATE TABLE escrow_transactions (
    id BIGSERIAL PRIMARY KEY,
    auction_id VARCHAR(255) NOT NULL,
    winner_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AUTHORIZED',
    authorization_id VARCHAR(255),
    capture_id VARCHAR(255),
    inspection_end_ts TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_escrow_transactions_auction_id ON escrow_transactions (auction_id);
CREATE INDEX idx_escrow_transactions_status ON escrow_transactions (status);
CREATE INDEX idx_escrow_transactions_inspection_end_ts ON escrow_transactions (inspection_end_ts);

-- Payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    auction_id VARCHAR(255) NOT NULL,
    payer_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    provider_ref VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_auction_id ON payments (auction_id);
CREATE INDEX idx_payments_payer_id ON payments (payer_id);
CREATE INDEX idx_payments_status ON payments (status);

-- Webhook events table for idempotency
CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    provider VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_events_event_id ON webhook_events (event_id);
CREATE INDEX idx_webhook_events_provider ON webhook_events (provider);

-- Fee schedules table for platform fees
CREATE TABLE fee_schedules (
    id BIGSERIAL PRIMARY KEY,
    fee_type VARCHAR(50) NOT NULL,
    calculation_type VARCHAR(50) NOT NULL,
    value DECIMAL(10,4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fee_schedules_fee_type_active ON fee_schedules (fee_type, active);

-- Invoices table
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    auction_id VARCHAR(255) NOT NULL,
    seller_id VARCHAR(255) NOT NULL,
    buyer_id VARCHAR(255),
    total_amount DECIMAL(10,2) NOT NULL,
    platform_fee DECIMAL(10,2),
    tax_amount DECIMAL(10,2),
    net_payout DECIMAL(10,2),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_auction_id ON invoices (auction_id);
CREATE INDEX idx_invoices_seller_id ON invoices (seller_id);
CREATE INDEX idx_invoices_status ON invoices (status);

-- Invoice items table
CREATE TABLE invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    item_type VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items (invoice_id);

-- Insert default platform fee: 5%
INSERT INTO fee_schedules (fee_type, calculation_type, value, active) VALUES ('PLATFORM_FEE', 'PERCENTAGE', 0.05, true);

-- Audit trail table for comprehensive logging
CREATE TABLE audit_trail (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(255),
    endpoint VARCHAR(255),
    details TEXT,
    entity_type VARCHAR(255),
    entity_id BIGINT
);

CREATE INDEX idx_audit_trail_user_id ON audit_trail (user_id);
CREATE INDEX idx_audit_trail_timestamp ON audit_trail (timestamp);
CREATE INDEX idx_audit_trail_entity_type_id ON audit_trail (entity_type, entity_id);