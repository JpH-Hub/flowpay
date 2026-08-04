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

    public Optional<Agent> findById(Long id) {
        String sql = "SELECT * FROM agents WHERE id = ?";
        List<Agent> agents = jdbcTemplate.query(sql, agentRowMapper, id);
        return agents.stream().findFirst();
    }

    public Optional<Agent> findByIdForUpdate(Long id) {
        String sql = "SELECT * FROM agents WHERE id = ? FOR UPDATE";
        List<Agent> agents = jdbcTemplate.query(sql, agentRowMapper, id);
        return agents.stream().findFirst();
    }

    public List<Agent> findAll() {
        String sql = "SELECT * FROM agents";
        return jdbcTemplate.query(sql, agentRowMapper);
    }

    public int countActiveTicketsByAgentId(Long agentId) {
        String sql = "SELECT COUNT(*) FROM tickets WHERE agent_id = ? AND status = ?";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, agentId, TicketStatus.IN_SERVICE.name());
        return count != null ? count : 0;
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