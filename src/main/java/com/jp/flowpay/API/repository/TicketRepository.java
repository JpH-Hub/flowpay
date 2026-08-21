package com.jp.flowpay.API.repository;

import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TicketStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TicketRepository {
    private final JdbcTemplate jdbcTemplate;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Ticket> ticketRowMapper = (rs, rowNum) -> {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getLong("id"));
        ticket.setConversationRef(rs.getString("conversation_ref"));
        ticket.setSubject(rs.getString("subject"));
        ticket.setStatus(TicketStatus.valueOf(rs.getString("status")));
        ticket.setTeamId(rs.getLong("team_id"));

        Long agentId = rs.getObject("agent_id") != null ? rs.getLong("agent_id") : null;

        ticket.setAgentId(agentId);
        ticket.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        ticket.setStartedAt(rs.getObject("started_at", LocalDateTime.class));
        ticket.setClosedAt(rs.getObject("closed_at", LocalDateTime.class));
        ticket.setRejectedAt(rs.getObject("rejected_at", LocalDateTime.class));
        ticket.setRejectionReason(rs.getString("rejection_reason"));

        return ticket;
    };

    public Ticket save(Ticket ticket) {
        String sql = """
                    INSERT INTO tickets (conversation_ref, subject, status, team_id, agent_id, created_at, started_at, rejected_at, rejection_reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, ticket.getConversationRef());
            ps.setString(2, ticket.getSubject());
            ps.setString(3, ticket.getStatus().name());
            ps.setLong(4, ticket.getTeamId());
            ps.setObject(5, ticket.getAgentId());
            ps.setObject(6, ticket.getCreatedAt() != null ? ticket.getCreatedAt() : LocalDateTime.now());
            ps.setObject(7, ticket.getStartedAt());
            ps.setObject(8, ticket.getRejectedAt());
            ps.setString(9, ticket.getRejectionReason());
            return ps;
        }, keyHolder);

        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            ticket.setId(((Number) keyHolder.getKeys().get("id")).longValue());
        }

        return ticket;
    }

    public Optional<Ticket> findById(Long id) {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        List<Ticket> tickets = jdbcTemplate.query(sql, ticketRowMapper, id);
        return tickets.stream().findFirst();
    }

    public Optional<Ticket> findByIdForUpdate(Long id) {
        String sql = "SELECT * FROM tickets WHERE id = ? FOR UPDATE";
        List<Ticket> tickets = jdbcTemplate.query(sql, ticketRowMapper, id);
        return tickets.stream().findFirst();
    }

    public void update(Ticket ticket) {
        String sql = """
                    UPDATE tickets 
                    SET status = ?, agent_id = ?, started_at = ?, closed_at = ?
                    WHERE id = ?
                """;
        jdbcTemplate.update(sql, ticket.getStatus().name(), ticket.getAgentId(), ticket.getStartedAt(),
                            ticket.getClosedAt(), ticket.getId());
    }

    public int countByStatusAndTeamId(TicketStatus status, Long teamId) {
        String sql = "SELECT COUNT(*) FROM tickets WHERE status = ? AND team_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, status.name(), teamId);
        return count != null ? count : 0;
    }

    public Optional<Ticket> findOldestQueuedByTeamIdForUpdate(Long teamId) {
        String sql = """
                SELECT * FROM tickets
                WHERE team_id = ? AND status = ?
                ORDER BY created_at ASC
                LIMIT 1
                FOR UPDATE
                """;

        List<Ticket> tickets = jdbcTemplate.query(sql, ticketRowMapper, teamId, TicketStatus.QUEUED.name());
        return tickets.stream().findFirst();
    }

    public List<Ticket> findByAgentIdAndStatus(Long agentId, TicketStatus status) {
        String sql = "SELECT * FROM tickets WHERE agent_id = ? AND status = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, ticketRowMapper, agentId, status.name());
    }

    public List<Ticket> findByTeamIdAndStatus(Long teamId, TicketStatus status) {
        String sql = "SELECT * FROM tickets WHERE team_id = ? AND status = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, ticketRowMapper, teamId, status.name());
    }

    public boolean existsByConversationRef(String conversationRef) {
        String sql = "SELECT COUNT(1) FROM tickets WHERE conversation_ref = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, conversationRef);
        return count != null && count > 0;
    }

}
