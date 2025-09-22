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