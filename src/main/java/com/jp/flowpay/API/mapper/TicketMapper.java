package com.jp.flowpay.API.mapper;

import com.jp.flowpay.API.dto.ticketDTO.response.CloseTicketResponseDTO;
import com.jp.flowpay.API.dto.ticketDTO.response.TicketResponseDTO;
import com.jp.flowpay.API.entity.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    public TicketResponseDTO toResponseDTO(Ticket ticket) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(ticket.getId());
        dto.setConversationRef(ticket.getConversationRef());
        dto.setSubject(ticket.getSubject());
        dto.setStatus(ticket.getStatus());
        dto.setAgentId(ticket.getAgentId());
        dto.setTeamId(ticket.getTeamId());
        dto.setStartedAt(ticket.getStartedAt());
        dto.setRejectedAt(ticket.getRejectedAt());
        dto.setRejectionReason(ticket.getRejectionReason());

        return dto;
    }

    public CloseTicketResponseDTO toCloseResponseDTO(Ticket ticket) {
        CloseTicketResponseDTO dto = new CloseTicketResponseDTO();
        dto.setTicketId(ticket.getId());
        dto.setStatus(ticket.getStatus());
        dto.setClosedAt(ticket.getClosedAt());

        return dto;
    }

}
