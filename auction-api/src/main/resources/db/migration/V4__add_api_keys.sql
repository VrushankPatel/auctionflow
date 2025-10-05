-- Add api_keys table for API key management

CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL,
    hashed_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for performance
CREATE INDEX idx_api_keys_hashed_key ON api_keys (hashed_key);
CREATE INDEX idx_api_keys_service_name ON api_keys (service_name);
CREATE INDEX idx_api_keys_revoked_at ON api_keys (revoked_at);