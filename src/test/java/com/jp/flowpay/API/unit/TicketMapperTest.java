package com.jp.flowpay.API.unit;

import com.jp.flowpay.API.dto.ticketDTO.response.CloseTicketResponseDTO;
import com.jp.flowpay.API.dto.ticketDTO.response.TicketResponseDTO;
import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TicketStatus;
import com.jp.flowpay.API.mapper.TicketMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TicketMapperTest {

    private final TicketMapper ticketMapper = new TicketMapper();

    @Test
    void shouldMapTicketToResponseDTO() {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setConversationRef("WHATS-123");
        ticket.setSubject("Dúvida");
        ticket.setStatus(TicketStatus.IN_SERVICE);
        ticket.setAgentId(10L);
        ticket.setTeamId(5L);
        ticket.setStartedAt(now);
        ticket.setRejectedAt(null);
        ticket.setRejectionReason(null);

        TicketResponseDTO dto = ticketMapper.toResponseDTO(ticket);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("WHATS-123", dto.getConversationRef());
        assertEquals("Dúvida", dto.getSubject());
        assertEquals(TicketStatus.IN_SERVICE, dto.getStatus());
        assertEquals(10L, dto.getAgentId());
        assertEquals(5L, dto.getTeamId());
        assertEquals(now, dto.getStartedAt());

        assertNull(dto.getRejectedAt());
        assertNull(dto.getRejectionReason());
    }

    @Test
    void shouldMapTicketToCloseResponseDTO() {
        LocalDateTime closedTime = LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.setId(2L);
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(closedTime);

        CloseTicketResponseDTO dto = ticketMapper.toCloseResponseDTO(ticket);

        assertNotNull(dto);
        assertEquals(2L, dto.getTicketId());
        assertEquals(TicketStatus.CLOSED, dto.getStatus());
        assertEquals(closedTime, dto.getClosedAt());
    }

}