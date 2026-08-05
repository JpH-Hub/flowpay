package com.jp.flowpay.API.controller;

import com.jp.flowpay.API.dto.ticketDTO.request.CreateTicketRequestDTO;
import com.jp.flowpay.API.dto.ticketDTO.response.CloseTicketResponseDTO;
import com.jp.flowpay.API.dto.ticketDTO.response.TicketResponseDTO;
import com.jp.flowpay.API.entity.Team;
import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.repository.TeamRepository;
import com.jp.flowpay.API.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TeamRepository teamRepository;

    public TicketController(TicketService ticketService, TeamRepository teamRepository) {
        this.ticketService = ticketService;
        this.teamRepository = teamRepository;
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@Valid @RequestBody CreateTicketRequestDTO request) {
        Ticket ticket = ticketService.assignTicket(request.getConversationRef(), request.getSubject());

        TicketResponseDTO dto = mapToDto(ticket);

        Team team = teamRepository.findById(ticket.getTeamId()).orElse(null);
        if (team != null) {
            dto.setTeam(team.getName());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<CloseTicketResponseDTO> closeTicket(@PathVariable("id") Long ticketId) {
        Ticket ticket = ticketService.closeTicket(ticketId);

        CloseTicketResponseDTO dto = new CloseTicketResponseDTO();
        dto.setTicketId(ticket.getId());
        dto.setStatus(ticket.getStatus());

        return ResponseEntity.ok(dto);
    }

    private TicketResponseDTO mapToDto(Ticket ticket) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(ticket.getId());
        dto.setConversationRef(ticket.getConversationRef());
        dto.setSubject(ticket.getSubject());
        dto.setStatus(ticket.getStatus());
        dto.setAgentId(ticket.getAgentId());
        return dto;
    }
}
