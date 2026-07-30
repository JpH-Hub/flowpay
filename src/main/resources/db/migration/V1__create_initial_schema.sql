CREATE TABLE teams (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE agents (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        team_id BIGINT NOT NULL,
                        CONSTRAINT fk_agents_team FOREIGN KEY (team_id) REFERENCES teams(id)
);
CREATE TABLE tickets (
                         id BIGSERIAL PRIMARY KEY,
                         conversation_ref VARCHAR(255) NOT NULL UNIQUE,
                         subject VARCHAR(255) NOT NULL,
                         status VARCHAR(30) NOT NULL,
                         team_id BIGINT NOT NULL,
                         agent_id BIGINT,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT fk_tickets_team FOREIGN KEY (team_id) REFERENCES teams(id),
                         CONSTRAINT fk_tickets_agent FOREIGN KEY (agent_id) REFERENCES agents(id)
);