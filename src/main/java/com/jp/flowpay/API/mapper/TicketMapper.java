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


        dto.setTeam(ticket.getTeamName());

        return dto;
    }

    public CloseTicketResponseDTO toCloseResponseDTO(Ticket ticket) {
        CloseTicketResponseDTO dto = new CloseTicketResponseDTO();

        dto.setTicketId(ticket.getId());
        dto.setStatus(ticket.getStatus());

        return dto;
    }

}
