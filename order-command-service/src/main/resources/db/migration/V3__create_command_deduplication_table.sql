-- Create command deduplication table for idempotency
CREATE TABLE command_deduplication (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    command_type VARCHAR(100) NOT NULL,
    response JSONB NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for efficient querying
CREATE INDEX idx_dedup_aggregate ON command_deduplication(aggregate_id);
CREATE INDEX idx_dedup_processed_at ON command_deduplication(processed_at);

-- Add comments for documentation
COMMENT ON TABLE command_deduplication IS 'Tracks processed commands for idempotency';
COMMENT ON COLUMN command_deduplication.idempotency_key IS 'Unique key provided by client for idempotency';
COMMENT ON COLUMN command_deduplication.aggregate_id IS 'Identifier of the aggregate affected by the command';
COMMENT ON COLUMN command_deduplication.command_type IS 'Type of command (CreateOrder, ApproveOrder, etc.)';
COMMENT ON COLUMN command_deduplication.response IS 'JSON response that was returned to the client';
COMMENT ON COLUMN command_deduplication.processed_at IS 'Timestamp when the command was processed';
