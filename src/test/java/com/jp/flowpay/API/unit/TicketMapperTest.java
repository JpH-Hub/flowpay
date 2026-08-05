package com.jp.flowpay.API.unit;

import com.jp.flowpay.API.dto.ticketDTO.response.CloseTicketResponseDTO;
import com.jp.flowpay.API.dto.ticketDTO.response.TicketResponseDTO;
import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TicketStatus;
import com.jp.flowpay.API.mapper.TicketMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TicketMapperTest {

    private final TicketMapper ticketMapper = new TicketMapper();

    @Test
    void shouldMapTicketToResponseDTO() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setConversationRef("WHATS-123");
        ticket.setSubject("Dúvida");
        ticket.setStatus(TicketStatus.IN_SERVICE);
        ticket.setAgentId(10L);
        ticket.setTeamName("Cartões");


        TicketResponseDTO dto = ticketMapper.toResponseDTO(ticket);


        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("WHATS-123", dto.getConversationRef());
        assertEquals("Dúvida", dto.getSubject());
        assertEquals(TicketStatus.IN_SERVICE, dto.getStatus());
        assertEquals(10L, dto.getAgentId());
        assertEquals("Cartões", dto.getTeam()); // Valida a nossa mágica do nome do time!
    }

    @Test
    void shouldMapTicketToCloseResponseDTO() {

        Ticket ticket = new Ticket();
        ticket.setId(2L);
        ticket.setStatus(TicketStatus.CLOSED);


        CloseTicketResponseDTO dto = ticketMapper.toCloseResponseDTO(ticket);


        assertNotNull(dto);
        assertEquals(2L, dto.getTicketId());
        assertEquals(TicketStatus.CLOSED, dto.getStatus());
    }
}
