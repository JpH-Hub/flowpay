package com.jp.flowpay.API.repository;

import com.jp.flowpay.API.entity.Agent;
import com.jp.flowpay.API.enums.TicketStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AgentRepository {

    private final JdbcTemplate jdbcTemplate;

    public AgentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Agent> agentRowMapper = (rs, rowNum) -> {
        Agent agent = new Agent();
        agent.setId(rs.getLong("id"));
        agent.setName(rs.getString("name"));
        agent.setTeamId(rs.getLong("team_id"));
        return agent;
    };

    public List<Agent> findByTeamId(Long teamId) {
        String sql = "SELECT * FROM agents WHERE team_id = ? ORDER BY id";
        return jdbcTemplate.query(sql, agentRowMapper, teamId);
    }

    public Optional<Agent> findAvailableByTeamId(Long teamId, int maxActivePerAgent) {
        String sql = """
            SELECT a.* FROM agents a
            WHERE a.team_id = ?
            AND (
                SELECT COUNT(*) FROM tickets t
                WHERE t.agent_id = a.id AND t.status = ?
            ) < ?
            ORDER BY (
                SELECT COUNT(*) FROM tickets t
                WHERE t.agent_id = a.id AND t.status = ?
            ) ASC, a.id ASC
            LIMIT 1
            """;

        String status = TicketStatus.IN_SERVICE.name();
        List<Agent> agents = jdbcTemplate.query(
                sql, agentRowMapper, teamId, status, maxActivePerAgent, status);
        return agents.stream().findFirst();
    }
}