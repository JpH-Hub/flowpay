package com.jp.flowpay.API.controller;

import com.jp.flowpay.API.dto.ticketDTO.request.CreateTicketRequestDTO;
import com.jp.flowpay.API.dto.ticketDTO.response.CloseTicketResponseDTO;
import com.jp.flowpay.API.dto.ticketDTO.response.TicketResponseDTO;
import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.mapper.TicketMapper;
import com.jp.flowpay.API.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
public class TicketController {
    private final TicketService ticketService;
    private final TicketMapper ticketMapper;

    public TicketController(TicketService ticketService, TicketMapper ticketMapper) {
        this.ticketService = ticketService;
        this.ticketMapper = ticketMapper;
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> createTicket(@Valid @RequestBody CreateTicketRequestDTO request) {

        Ticket ticket = ticketService.assignTicket(request.getConversationRef(), request.getSubject());


        TicketResponseDTO dto = ticketMapper.toResponseDTO(ticket);


        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<CloseTicketResponseDTO> closeTicket(@PathVariable("id") Long ticketId) {

        Ticket ticket = ticketService.closeTicket(ticketId);


        CloseTicketResponseDTO dto = ticketMapper.toCloseResponseDTO(ticket);


        return ResponseEntity.ok(dto);
    }
}
