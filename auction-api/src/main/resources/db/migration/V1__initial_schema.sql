-- Initial schema for Auction Flow
-- Core tables as per PRODUCT_SPECIFICATION.md

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    kyc_status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Sellers table
CREATE TABLE sellers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    rating DECIMAL(3,2),
    payment_info JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Items table
CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL REFERENCES sellers(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT,
    metadata_json JSONB,
    images JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Auctions table
CREATE TABLE auctions (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES items(id),
    type VARCHAR(50) NOT NULL,
    start_ts TIMESTAMP WITH TIME ZONE NOT NULL,
    end_ts TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reserve_price DECIMAL(10,2),
    buy_now_price DECIMAL(10,2),
    increment_strategy_id BIGINT,
    extension_policy_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Bids table
CREATE TABLE bids (
    id BIGSERIAL PRIMARY KEY,
    auction_id BIGINT NOT NULL REFERENCES auctions(id),
    bidder_id BIGINT NOT NULL REFERENCES users(id),
    amount DECIMAL(10,2) NOT NULL,
    server_ts TIMESTAMP WITH TIME ZONE NOT NULL,
    seq_no BIGINT NOT NULL,
    accepted_bool BOOLEAN NOT NULL DEFAULT FALSE
);

-- Watchers/Watchlist table
CREATE TABLE watchers (
    user_id BIGINT NOT NULL REFERENCES users(id),
    auction_id BIGINT NOT NULL REFERENCES auctions(id),
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, auction_id)
);

-- Auction events table (append-only event log) - partitioned by month
CREATE TABLE auction_events (
    id BIGSERIAL NOT NULL,
    auction_id BIGINT NOT NULL REFERENCES auctions(id),
    type VARCHAR(255) NOT NULL,
    payload_json JSONB,
    ts TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (ts);

-- Create initial partitions for current and next 12 months
DO $$
DECLARE
    current_month DATE := DATE_TRUNC('month', CURRENT_DATE);
    i INT := 0;
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
BEGIN
    FOR i IN 0..12 LOOP
        start_date := current_month + INTERVAL '1 month' * i;
        end_date := start_date + INTERVAL '1 month';
        partition_name := 'auction_events_' || TO_CHAR(start_date, 'YYYY_MM');
        EXECUTE 'CREATE TABLE ' || partition_name || ' PARTITION OF auction_events FOR VALUES FROM (''' || start_date || ''') TO (''' || end_date || ''');';
    END LOOP;
END $$;

-- Payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    auction_id BIGINT NOT NULL REFERENCES auctions(id),
    payer_id BIGINT NOT NULL REFERENCES users(id),
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    provider_ref VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Config/Reference tables
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT REFERENCES categories(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE bid_increments (
    id BIGSERIAL PRIMARY KEY,
    strategy_name VARCHAR(255) NOT NULL,
    min_amount DECIMAL(10,2),
    max_amount DECIMAL(10,2),
    increment DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE auction_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE extension_policies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_auctions_status_end_ts ON auctions (status, end_ts);
CREATE INDEX idx_auctions_seller_id_status ON auctions (item_id, status); -- since item_id links to seller
CREATE INDEX idx_bids_auction_id_server_ts_accepted ON bids (auction_id, server_ts DESC, accepted_bool);
CREATE INDEX idx_bids_bidder_id_server_ts ON bids (bidder_id, server_ts DESC);
CREATE INDEX idx_auction_events_auction_id_ts_type ON auction_events (auction_id, ts, type);
CREATE INDEX idx_payments_auction_id_status ON payments (auction_id, status);