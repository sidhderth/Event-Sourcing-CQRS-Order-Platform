-- Create snapshots table for aggregate state snapshots
CREATE TABLE snapshots (
    aggregate_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL,
    state JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create index on version for efficient querying
CREATE INDEX idx_snapshots_version ON snapshots(version);

-- Add comments for documentation
COMMENT ON TABLE snapshots IS 'Aggregate snapshots for optimizing event replay';
COMMENT ON COLUMN snapshots.aggregate_id IS 'Identifier of the aggregate';
COMMENT ON COLUMN snapshots.aggregate_type IS 'Type of aggregate (Order)';
COMMENT ON COLUMN snapshots.version IS 'Version number of the aggregate at snapshot time';
COMMENT ON COLUMN snapshots.state IS 'JSON representation of the aggregate state';
COMMENT ON COLUMN snapshots.created_at IS 'Timestamp when the snapshot was created';
