-- Create events table for event store
CREATE TABLE events (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL,
    payload JSONB NOT NULL,
    metadata JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    trace_id VARCHAR(32),
    actor VARCHAR(255),
    CONSTRAINT unique_aggregate_version UNIQUE (aggregate_id, version)
);

-- Create indexes for efficient querying
CREATE INDEX idx_events_aggregate_id ON events(aggregate_id);
CREATE INDEX idx_events_occurred_at ON events(occurred_at);
CREATE INDEX idx_events_event_type ON events(event_type);

-- Add comment for documentation
COMMENT ON TABLE events IS 'Event store containing all domain events';
COMMENT ON COLUMN events.event_id IS 'Unique identifier for the event';
COMMENT ON COLUMN events.aggregate_id IS 'Identifier of the aggregate (Order) this event belongs to';
COMMENT ON COLUMN events.event_type IS 'Type of event (OrderCreated, OrderApproved, etc.)';
COMMENT ON COLUMN events.version IS 'Version number of the aggregate after this event';
COMMENT ON COLUMN events.payload IS 'JSON payload containing event data';
COMMENT ON COLUMN events.metadata IS 'Additional metadata about the event';
COMMENT ON COLUMN events.occurred_at IS 'Timestamp when the event occurred';
COMMENT ON COLUMN events.trace_id IS 'Distributed tracing correlation ID';
COMMENT ON COLUMN events.actor IS 'User or system that triggered the event';
