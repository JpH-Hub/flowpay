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
        return ticket;
    };

    public Ticket save(Ticket ticket) {
        String sql = """
            INSERT INTO tickets (conversation_ref, subject, status, team_id, agent_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
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

    public List<Ticket> findAll() {
        String sql = "SELECT * FROM tickets";
        return jdbcTemplate.query(sql, ticketRowMapper);
    }

    public void update(Ticket ticket) {
        String sql = """
            UPDATE tickets 
            SET status = ?, agent_id = ? 
            WHERE id = ?
        """;
        jdbcTemplate.update(sql, ticket.getStatus().name(), ticket.getAgentId(), ticket.getId());
    }

}
