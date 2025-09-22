-- Add compliance-related tables for KYC/AML

-- Compliance checks table
CREATE TABLE compliance_checks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    check_type VARCHAR(50) NOT NULL, -- IDENTITY, DOCUMENT, RISK_ASSESSMENT, TRANSACTION_MONITORING
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PASSED, FAILED, REQUIRES_REVIEW
    risk_score DECIMAL(5,2), -- 0.00 to 100.00, higher means higher risk
    details JSONB, -- Additional details about the check
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Document uploads table for KYC documents
CREATE TABLE document_uploads (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    document_type VARCHAR(50) NOT NULL, -- PASSPORT, DRIVERS_LICENSE, ID_CARD, PROOF_OF_ADDRESS, etc.
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL, -- Path to stored file
    mime_type VARCHAR(100),
    file_size BIGINT,
    upload_status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED', -- UPLOADED, VERIFIED, REJECTED
    verification_result JSONB, -- Results from document verification service
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    verified_at TIMESTAMP WITH TIME ZONE
);

-- Indexes
CREATE INDEX idx_compliance_checks_user_id ON compliance_checks (user_id);
CREATE INDEX idx_compliance_checks_status ON compliance_checks (status);
CREATE INDEX idx_compliance_checks_type_status ON compliance_checks (check_type, status);
CREATE INDEX idx_document_uploads_user_id ON document_uploads (user_id);
CREATE INDEX idx_document_uploads_status ON document_uploads (upload_status);

-- Function to update updated_at timestamp for compliance_checks
CREATE OR REPLACE FUNCTION update_compliance_check_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for compliance_checks
CREATE TRIGGER trigger_update_compliance_check_updated_at
    BEFORE UPDATE ON compliance_checks
    FOR EACH ROW
    EXECUTE FUNCTION update_compliance_check_updated_at();