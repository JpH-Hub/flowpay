ALTER TABLE tickets
    ADD COLUMN started_at TIMESTAMP,
ADD COLUMN closed_at TIMESTAMP,
ADD COLUMN rejected_at TIMESTAMP,
ADD COLUMN rejection_reason VARCHAR(50);


CREATE INDEX idx_tickets_active
    ON tickets(team_id, status)
    WHERE status IN ('IN_SERVICE', 'QUEUED');