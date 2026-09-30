
CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY,
                               aggregate_type VARCHAR(50) NOT NULL,
                               aggregate_id VARCHAR(36) NOT NULL,
                               type VARCHAR(100) NOT NULL,
                               payload JSONB NOT NULL,
                               status VARCHAR(20) NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);