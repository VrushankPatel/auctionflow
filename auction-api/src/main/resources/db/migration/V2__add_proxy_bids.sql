-- Add proxy_bids table for automatic bidding feature

CREATE TABLE proxy_bids (
    id BIGSERIAL PRIMARY KEY,
    auction_id VARCHAR(255) NOT NULL REFERENCES auctions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    max_bid DECIMAL(10,2) NOT NULL CHECK (max_bid > 0),
    current_bid DECIMAL(10,2) DEFAULT 0 CHECK (current_bid >= 0),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'OUTBID', 'WON', 'CANCELLED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (auction_id, user_id)
);

-- Indexes for performance
CREATE INDEX idx_proxy_bids_auction_id_status ON proxy_bids (auction_id, status);
CREATE INDEX idx_proxy_bids_user_id_status ON proxy_bids (user_id, status);
CREATE INDEX idx_proxy_bids_updated_at ON proxy_bids (updated_at);

-- Function to update updated_at timestamp
-- CREATE OR REPLACE FUNCTION update_proxy_bid_updated_at()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     NEW.updated_at = NOW();
--     RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at
-- CREATE TRIGGER trigger_update_proxy_bid_updated_at
--     BEFORE UPDATE ON proxy_bids
--     FOR EACH ROW
--     EXECUTE FUNCTION update_proxy_bid_updated_at();