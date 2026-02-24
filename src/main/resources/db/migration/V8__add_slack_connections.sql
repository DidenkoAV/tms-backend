-- Slack connections (per group)
CREATE TABLE slack_connections (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    webhook_url TEXT NOT NULL,
    channel_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_slack_connections_group UNIQUE (group_id)
);

CREATE INDEX idx_slack_connections_group ON slack_connections(group_id);

