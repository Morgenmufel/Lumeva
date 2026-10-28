CREATE TABLE outbox_events (
                               id            UUID         PRIMARY KEY,
                               aggregate_type VARCHAR(64)  NOT NULL,
                               aggregate_id   VARCHAR(64)  NOT NULL,
                               type          VARCHAR(128) NOT NULL,
                               payload       BYTEA        NOT NULL,
                               created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_events_created_at ON outbox_events (created_at);