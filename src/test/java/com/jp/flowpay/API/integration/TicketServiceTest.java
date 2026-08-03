package com.jp.flowpay.API.integration;

import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TicketStatus;
import com.jp.flowpay.API.exception.InvalidTicketStatusException;
import com.jp.flowpay.API.exception.TeamNotFoundException;
import com.jp.flowpay.API.exception.TicketNotFoundException;
import com.jp.flowpay.API.repository.TicketRepository;
import com.jp.flowpay.API.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("Deve atribuir chamado a um atendente disponível")
    void shouldAssignTicketToAvailableAgent() {
        Ticket ticket = ticketService.createTicket("conv-001", "Cartões");

        assertEquals(TicketStatus.IN_SERVICE, ticket.getStatus());
        assertEquals(1L, ticket.getTeamId());
        assertNotNull(ticket.getAgentId());
    }

    @Test
    @DisplayName("Deve colocar chamado na fila quando todos os atendentes do time estiverem ocupados")
    void shouldQueueTicketWhenTeamIsFull() {
        ticketService.createTicket("conv-002", "Cartões");
        ticketService.createTicket("conv-003", "Cartões");
        ticketService.createTicket("conv-004", "Cartões");

        Ticket queuedTicket = ticketService.createTicket("conv-005", "Cartões");

        assertEquals(TicketStatus.QUEUED, queuedTicket.getStatus());
        assertNull(queuedTicket.getAgentId());
    }

    @Test
    @DisplayName("Deve rejeitar chamado quando a fila global estiver cheia")
    void shouldRejectTicketWhenQueueIsFull() {
        ticketService.createTicket("conv-006", "Cartões");
        ticketService.createTicket("conv-007", "Cartões");
        ticketService.createTicket("conv-008", "Cartões");
        ticketService.createTicket("conv-009", "Cartões");

        ticketService.createTicket("conv-010", "Empréstimos");
        ticketService.createTicket("conv-011", "Empréstimos");
        ticketService.createTicket("conv-012", "Empréstimos");

        ticketService.createTicket("conv-013", "Cartões");
        ticketService.createTicket("conv-014", "Empréstimos");
        ticketService.createTicket("conv-015", "Outros Assuntos");

        Ticket rejectedTicket = ticketService.createTicket("conv-016", "Cartões");

        assertEquals(TicketStatus.REJECTED, rejectedTicket.getStatus());
        assertNull(rejectedTicket.getAgentId());
    }

    @Test
    @DisplayName("Deve promover chamado da fila ao finalizar um atendimento")
    void shouldPromoteQueuedTicketWhenClosingActiveTicket() {
        Ticket first = ticketService.createTicket("conv-017", "Cartões");
        ticketService.createTicket("conv-018", "Cartões");
        ticketService.createTicket("conv-019", "Cartões");
        Ticket queuedTicket = ticketService.createTicket("conv-020", "Cartões");

        assertEquals(TicketStatus.QUEUED, queuedTicket.getStatus());

        ticketService.closeTicket(first.getId());

        Ticket promoted = ticketRepository.findById(queuedTicket.getId()).orElseThrow();
        assertEquals(TicketStatus.IN_SERVICE, promoted.getStatus());
        assertNotNull(promoted.getAgentId());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o time não existir")
    void shouldThrowWhenTeamNotFound() {
        assertThrows(TeamNotFoundException.class,
                () -> ticketService.createTicket("conv-022", "Time Inexistente"));
    }

    @Test
    @DisplayName("Deve lançar exceção ao fechar chamado inexistente")
    void shouldThrowWhenClosingUnknownTicket() {
        assertThrows(TicketNotFoundException.class,
                () -> ticketService.closeTicket(999L));
    }

    @Test
    @DisplayName("Deve lançar exceção ao fechar chamado já finalizado")
    void shouldThrowWhenClosingAlreadyClosedTicket() {
        Ticket ticket = ticketService.createTicket("conv-023", "Outros Assuntos");
        ticketService.closeTicket(ticket.getId());

        assertThrows(InvalidTicketStatusException.class,
                () -> ticketService.closeTicket(ticket.getId()));
    }
}
