package com.jp.flowpay.API.unit;

import com.jp.flowpay.API.entity.Agent;
import com.jp.flowpay.API.entity.Team;
import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TeamEnum;
import com.jp.flowpay.API.enums.TicketStatus;
import com.jp.flowpay.API.exception.InvalidTicketStatusException;
import com.jp.flowpay.API.exception.TeamNotFoundException;
import com.jp.flowpay.API.exception.TicketNotFoundException;
import com.jp.flowpay.API.repository.AgentRepository;
import com.jp.flowpay.API.repository.TeamRepository;
import com.jp.flowpay.API.repository.TicketRepository;
import com.jp.flowpay.API.service.TeamRoutingService;
import com.jp.flowpay.API.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TeamRoutingService teamRoutingService;

    @InjectMocks
    private TicketService ticketService;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    private Team team;
    private Agent agent;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(ticketService, "maxQueueSize", 3);
        ReflectionTestUtils.setField(ticketService, "maxActivePerAgent", 3);

        team = new Team();
        team.setId(1L);
        team.setName("Cartoes");

        agent = new Agent();
        agent.setId(10L);
    }


    @Test
    @DisplayName("Deve criar ticket IN_SERVICE quando houver atendente disponível")
    void assignTicket_AgentAvailable_ShouldCreateInService() {

        String ref = "chat-123";
        String subject = "Problema com meu cartao";

        when(teamRoutingService.determineTeam(subject)).thenReturn(TeamEnum.CREDIT_CARDS);
        when(teamRepository.findByNameIgnoreCase(TeamEnum.CREDIT_CARDS.getTeamName())).thenReturn(Optional.of(team));
        when(agentRepository.findAvailableByTeamId(1L, 3)).thenReturn(Optional.of(agent));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));


        Ticket result = ticketService.assignTicket(ref, subject);


        assertNotNull(result);
        assertEquals(TicketStatus.IN_SERVICE, result.getStatus());
        assertEquals(20L, result.getAgentId());
        assertEquals(1L, result.getTeamId());

        verify(ticketRepository).save(any(Ticket.class));
        verify(ticketRepository, never()).countByStatusAndTeamId(any(), any());
    }

    @Test
    @DisplayName("Deve criar ticket QUEUED quando não houver atendente, mas a fila não estiver cheia")
    void assignTicket_AgentNotAvailable_QueueNotFull_ShouldCreateQueued() {

        String subject = "cartao";

        when(teamRoutingService.determineTeam(subject)).thenReturn(TeamEnum.CREDIT_CARDS);
        when(teamRepository.findByNameIgnoreCase(TeamEnum.CREDIT_CARDS.getTeamName())).thenReturn(Optional.of(team));
        when(agentRepository.findAvailableByTeamId(1L, 3)).thenReturn(Optional.empty());
        when(ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, 1L)).thenReturn(2);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));


        Ticket result = ticketService.assignTicket("chat-123", subject);


        assertEquals(TicketStatus.QUEUED, result.getStatus());
        assertNull(result.getAgentId());

        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Deve criar ticket REJECTED quando não houver atendente e a fila estiver cheia")
    void assignTicket_AgentNotAvailable_QueueFull_ShouldCreateRejected() {

        String subject = "cartao";

        when(teamRoutingService.determineTeam(subject)).thenReturn(TeamEnum.CREDIT_CARDS);
        when(teamRepository.findByNameIgnoreCase(TeamEnum.CREDIT_CARDS.getTeamName())).thenReturn(Optional.of(team));
        when(agentRepository.findAvailableByTeamId(1L, 3)).thenReturn(Optional.empty());
        when(ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, 1L)).thenReturn(3);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));


        Ticket result = ticketService.assignTicket("chat-123", subject);


        assertEquals(TicketStatus.REJECTED, result.getStatus());
        assertNull(result.getAgentId());
    }

    @Test
    @DisplayName("Deve lançar exception ao tentar atribuir ticket e a equipe não existir no banco")
    void assignTicket_TeamNotFound_ShouldThrowException() {

        when(teamRoutingService.determineTeam(anyString())).thenReturn(TeamEnum.CREDIT_CARDS);
        when(teamRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());


        assertThrows(TeamNotFoundException.class, () -> ticketService.assignTicket("chat-123", "cartao"));
        verify(agentRepository, never()).findAvailableByTeamId(any(), anyInt());
    }


    @Test
    @DisplayName("Deve fechar ticket QUEUED e não promover ninguém da fila (pois não liberou atendente)")
    void closeTicket_WhenStatusQueued_ShouldCloseAndNotPromote() {

        Ticket ticket = new Ticket();
        ticket.setId(99L);
        ticket.setStatus(TicketStatus.QUEUED);
        ticket.setTeamId(1L);

        when(ticketRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(ticket));


        Ticket result = ticketService.closeTicket(99L);


        assertEquals(TicketStatus.CLOSED, result.getStatus());
        verify(ticketRepository).update(ticket);

        verify(ticketRepository, never()).findOldestQueuedByTeamIdForUpdate(any());
    }

    @Test
    @DisplayName("Deve fechar ticket IN_SERVICE e promover o próximo da fila com sucesso")
    void closeTicket_WhenStatusInService_ShouldCloseAndPromoteNext() {

        Ticket ticketEmAtendimento = new Ticket();
        ticketEmAtendimento.setId(99L);
        ticketEmAtendimento.setStatus(TicketStatus.IN_SERVICE);
        ticketEmAtendimento.setTeamId(1L);

        Ticket ticketNaFila = new Ticket();
        ticketNaFila.setId(100L);
        ticketNaFila.setStatus(TicketStatus.QUEUED);
        ticketNaFila.setTeamId(1L);

        when(ticketRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(ticketEmAtendimento));


        when(ticketRepository.findOldestQueuedByTeamIdForUpdate(1L)).thenReturn(Optional.of(ticketNaFila));
        when(agentRepository.findAvailableByTeamId(1L, 3)).thenReturn(Optional.of(agent));


        Ticket result = ticketService.closeTicket(99L);


        assertEquals(TicketStatus.CLOSED, result.getStatus());


        verify(ticketRepository, times(2)).update(ticketCaptor.capture());

        Ticket ticketPromovido = ticketCaptor.getAllValues().get(1);
        assertEquals(TicketStatus.IN_SERVICE, ticketPromovido.getStatus());
        assertEquals(10L, ticketPromovido.getAgentId());
    }

    @Test
    @DisplayName("Deve fechar ticket IN_SERVICE mas não promover ninguém se a fila estiver vazia")
    void closeTicket_WhenStatusInService_EmptyQueue_ShouldCloseOnly() {

        Ticket ticketEmAtendimento = new Ticket();
        ticketEmAtendimento.setId(99L);
        ticketEmAtendimento.setStatus(TicketStatus.IN_SERVICE);
        ticketEmAtendimento.setTeamId(1L);

        when(ticketRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(ticketEmAtendimento));
        when(ticketRepository.findOldestQueuedByTeamIdForUpdate(1L)).thenReturn(Optional.empty());


        ticketService.closeTicket(99L);


        verify(ticketRepository, times(1)).update(any(Ticket.class));
        verify(agentRepository, never()).findAvailableByTeamId(any(), anyInt());
    }

    @Test
    @DisplayName("Deve lançar exception ao tentar fechar ticket que não existe")
    void closeTicket_NotFound_ShouldThrowException() {

        when(ticketRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());


        assertThrows(TicketNotFoundException.class, () -> ticketService.closeTicket(1L));
    }

    @Test
    @DisplayName("Deve lançar exception ao tentar fechar ticket que já está CLOSED ou REJECTED")
    void closeTicket_InvalidStatus_ShouldThrowException() {

        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.CLOSED);

        when(ticketRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(ticket));


        assertThrows(InvalidTicketStatusException.class, () -> ticketService.closeTicket(1L));
    }
}