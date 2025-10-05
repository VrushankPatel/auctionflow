-- Add soft delete columns to existing tables

ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletedby BIGINT;

ALTER TABLE auctions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS deletedby BIGINT;

ALTER TABLE bids ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE bids ADD COLUMN IF NOT EXISTS deletedby BIGINT;

ALTER TABLE items ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE items ADD COLUMN IF NOT EXISTS deletedby BIGINT;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);
CREATE INDEX IF NOT EXISTS idx_auctions_deleted_at ON auctions (deleted_at);
CREATE INDEX IF NOT EXISTS idx_bids_deleted_at ON bids (deleted_at);
CREATE INDEX IF NOT EXISTS idx_items_deleted_at ON items (deleted_at);