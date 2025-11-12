-- Create outbox table for transactional publishing pattern
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);

-- Create indexes for efficient querying
CREATE INDEX idx_outbox_status ON outbox(status);
CREATE INDEX idx_outbox_created_at ON outbox(created_at);
CREATE INDEX idx_outbox_aggregate_id ON outbox(aggregate_id);

-- Add comments for documentation
COMMENT ON TABLE outbox IS 'Transactional outbox for reliable event publishing to Kafka';
COMMENT ON COLUMN outbox.id IS 'Unique identifier for the outbox record';
COMMENT ON COLUMN outbox.aggregate_id IS 'Identifier of the aggregate that generated the event';
COMMENT ON COLUMN outbox.event_type IS 'Type of event to be published';
COMMENT ON COLUMN outbox.payload IS 'JSON payload of the event';
COMMENT ON COLUMN outbox.created_at IS 'Timestamp when the event was added to outbox';
COMMENT ON COLUMN outbox.published_at IS 'Timestamp when the event was successfully published to Kafka';
COMMENT ON COLUMN outbox.status IS 'Status of the outbox record (PENDING, PUBLISHED, FAILED)';
