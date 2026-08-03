package com.jp.flowpay.API.integration;

import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TicketStatus;
import com.jp.flowpay.API.exception.InvalidTicketStatusException;
import com.jp.flowpay.API.exception.TeamNotFoundException;
import com.jp.flowpay.API.exception.TicketNotFoundException;
import com.jp.flowpay.API.repository.TicketRepository;
import com.jp.flowpay.API.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketServiceTest {

    private static final Long TEAM_CARTOES = 1L;
    private static final Long TEAM_EMPRESTIMOS = 2L;
    private static final Long TEAM_OUTROS = 3L;

    private static final int MAX_ACTIVE_PER_AGENT = 3;
    private static final int AGENTS_PER_TEAM = 3;
    private static final int MAX_IN_SERVICE_PER_TEAM = MAX_ACTIVE_PER_AGENT * AGENTS_PER_TEAM;
    private static final int MAX_QUEUE_PER_TEAM = 3;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Nested
    @DisplayName("assignTicket")
    class AssignTicket {

        @Test
        @DisplayName("deve atribuir chamado a um atendente disponível do time informado")
        void shouldAssignToAvailableAgent() {
            Ticket savedTicket = ticketService.assignTicket("conv-001", "Problema no cartão", TEAM_CARTOES);

            Ticket ticket = ticketRepository.findById(savedTicket.getId()).orElseThrow();

            assertNotNull(ticket.getId());
            assertEquals("conv-001", ticket.getConversationRef());
            assertEquals("Problema no cartão", ticket.getSubject());
            assertEquals(TicketStatus.IN_SERVICE, ticket.getStatus());
            assertEquals(TEAM_CARTOES, ticket.getTeamId());
            assertNotNull(ticket.getAgentId());
            assertNotNull(ticket.getCreatedAt());
        }

        @Test
        @DisplayName("deve rotear pelo teamId e ignorar o subject na escolha do time")
        void shouldRouteByTeamIdRegardlessOfSubject() {
            Ticket ticket = ticketService.assignTicket("conv-002", "Qualquer assunto", TEAM_EMPRESTIMOS);

            assertEquals(TEAM_EMPRESTIMOS, ticket.getTeamId());
            assertEquals(TicketStatus.IN_SERVICE, ticket.getStatus());
        }

        @Test
        @DisplayName("deve distribuir chamados entre atendentes diferentes do mesmo time")
        void shouldAssignDifferentAgentsWithinTeam() {
            Ticket first = ticketService.assignTicket("conv-003", "A", TEAM_CARTOES);
            Ticket second = ticketService.assignTicket("conv-004", "B", TEAM_CARTOES);
            Ticket third = ticketService.assignTicket("conv-005", "C", TEAM_CARTOES);

            Set<Long> agentIds = Set.of(first.getAgentId(), second.getAgentId(), third.getAgentId());
            assertEquals(3, agentIds.size(), "Os três primeiros chamados devem ir para atendentes distintos");
        }

        @Test
        @DisplayName("deve permitir até 3 chamados simultâneos por atendente")
        void shouldAllowUpToThreeTicketsPerAgent() {
            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                Ticket ticket = ticketService.assignTicket("conv-006-" + i, "Assunto " + i, TEAM_CARTOES);
                assertEquals(TicketStatus.IN_SERVICE, ticket.getStatus());
            }

            assertEquals(MAX_IN_SERVICE_PER_TEAM, ticketRepository.countByStatus(TicketStatus.IN_SERVICE));

            Ticket queued = ticketService.assignTicket("conv-006-queued", "Fila", TEAM_CARTOES);
            assertEquals(TicketStatus.QUEUED, queued.getStatus());
        }

        @Test
        @DisplayName("deve colocar na fila quando todos os atendentes do time estiverem ocupados")
        void shouldQueueWhenTeamIsFull() {
            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                ticketService.assignTicket("conv-007-" + i, "Assunto " + i, TEAM_CARTOES);
            }

            Ticket queued = ticketService.assignTicket("conv-009", "D", TEAM_CARTOES);

            assertEquals(TicketStatus.QUEUED, queued.getStatus());
            assertNull(queued.getAgentId());
            assertEquals(1, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));
        }

        @Test
        @DisplayName("deve rejeitar chamado quando a fila do time estiver cheia")
        void shouldRejectWhenTeamQueueIsFull() {
            fillTeamToCapacity(TEAM_CARTOES, "conv-010");

            assertEquals(MAX_QUEUE_PER_TEAM, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));

            Ticket rejected = ticketService.assignTicket("conv-040", "Rejeitado", TEAM_CARTOES);

            assertEquals(TicketStatus.REJECTED, rejected.getStatus());
            assertNull(rejected.getAgentId());
            assertEquals(MAX_QUEUE_PER_TEAM, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));
        }

        @Test
        @DisplayName("deve manter filas independentes entre times")
        void shouldKeepIndependentQueuesPerTeam() {
            fillTeamToCapacity(TEAM_CARTOES, "conv-020");

            Ticket accepted = ticketService.assignTicket("conv-021", "Empréstimos", TEAM_EMPRESTIMOS);

            assertEquals(TicketStatus.IN_SERVICE, accepted.getStatus());
            assertEquals(MAX_QUEUE_PER_TEAM, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));
            assertEquals(0, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_EMPRESTIMOS));
        }

        @Test
        @DisplayName("deve permitir atendimento simultâneo em times diferentes")
        void shouldAllowConcurrentServiceAcrossTeams() {
            fillTeamToCapacity(TEAM_CARTOES, "conv-050");
            fillTeamToCapacity(TEAM_EMPRESTIMOS, "conv-060");
            fillTeamToCapacity(TEAM_OUTROS, "conv-070");

            assertEquals(MAX_IN_SERVICE_PER_TEAM * 3, ticketRepository.countByStatus(TicketStatus.IN_SERVICE));
            assertEquals(MAX_QUEUE_PER_TEAM * 3, ticketRepository.countByStatus(TicketStatus.QUEUED));
        }

        @Test
        @DisplayName("deve lançar exceção quando o teamId não existir")
        void shouldThrowWhenTeamNotFound() {
            assertThrows(TeamNotFoundException.class,
                    () -> ticketService.assignTicket("conv-080", "Assunto", 999L));
        }
    }

    @Nested
    @DisplayName("closeTicket")
    class CloseTicket {

        @Test
        @DisplayName("deve finalizar chamado em atendimento")
        void shouldCloseInServiceTicket() {
            Ticket ticket = ticketService.assignTicket("conv-100", "Assunto", TEAM_CARTOES);

            Ticket closed = ticketService.closeTicket(ticket.getId());

            assertEquals(TicketStatus.CLOSED, closed.getStatus());
        }

        @Test
        @DisplayName("deve finalizar chamado na fila sem promover outros")
        void shouldCloseQueuedTicketWithoutPromotion() {
            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                ticketService.assignTicket("conv-110-" + i, "Assunto " + i, TEAM_CARTOES);
            }
            Ticket queued = ticketService.assignTicket("conv-114", "Na fila", TEAM_CARTOES);

            Ticket closed = ticketService.closeTicket(queued.getId());

            assertEquals(TicketStatus.CLOSED, closed.getStatus());
            assertEquals(0, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));
            assertEquals(MAX_IN_SERVICE_PER_TEAM, ticketRepository.countByStatus(TicketStatus.IN_SERVICE));
        }

        @Test
        @DisplayName("deve promover o chamado mais antigo da fila do mesmo time")
        void shouldPromoteOldestQueuedTicketFromSameTeam() {
            List<Ticket> activeCartoes = new ArrayList<>();
            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                activeCartoes.add(ticketService.assignTicket("conv-120-" + i, "A" + i, TEAM_CARTOES));
            }
            Ticket oldestQueued = ticketService.assignTicket("conv-123", "D", TEAM_CARTOES);

            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                ticketService.assignTicket("conv-125-" + i, "E" + i, TEAM_EMPRESTIMOS);
            }
            Ticket emprestimosQueued = ticketService.assignTicket("conv-127", "H", TEAM_EMPRESTIMOS);

            ticketService.closeTicket(activeCartoes.get(0).getId());

            Ticket promotedCartoes = ticketRepository.findById(oldestQueued.getId()).orElseThrow();
            Ticket stillQueuedEmprestimos = ticketRepository.findById(emprestimosQueued.getId()).orElseThrow();

            assertEquals(TicketStatus.IN_SERVICE, promotedCartoes.getStatus());
            assertNotNull(promotedCartoes.getAgentId());
            assertEquals(TicketStatus.QUEUED, stillQueuedEmprestimos.getStatus());
        }

        @Test
        @DisplayName("deve liberar vaga e aceitar novo chamado após fechar atendimento")
        void shouldAcceptNewTicketAfterClosingOne() {
            Ticket first = ticketService.assignTicket("conv-130", "A", TEAM_CARTOES);
            ticketService.assignTicket("conv-131", "B", TEAM_CARTOES);
            ticketService.assignTicket("conv-132", "C", TEAM_CARTOES);

            ticketService.closeTicket(first.getId());

            Ticket newTicket = ticketService.assignTicket("conv-134", "E", TEAM_CARTOES);

            assertEquals(TicketStatus.IN_SERVICE, newTicket.getStatus());
            assertNotNull(newTicket.getAgentId());
        }

        @Test
        @DisplayName("deve lançar exceção ao fechar chamado inexistente")
        void shouldThrowWhenTicketNotFound() {
            assertThrows(TicketNotFoundException.class,
                    () -> ticketService.closeTicket(999L));
        }

        @Test
        @DisplayName("deve lançar exceção ao fechar chamado já finalizado")
        void shouldThrowWhenAlreadyClosed() {
            Ticket ticket = ticketService.assignTicket("conv-140", "Assunto", TEAM_OUTROS);
            ticketService.closeTicket(ticket.getId());

            assertThrows(InvalidTicketStatusException.class,
                    () -> ticketService.closeTicket(ticket.getId()));
        }

        @Test
        @DisplayName("deve lançar exceção ao fechar chamado rejeitado")
        void shouldThrowWhenClosingRejectedTicket() {
            fillTeamToCapacity(TEAM_CARTOES, "conv-150");

            Ticket rejected = ticketService.assignTicket("conv-180", "Rejeitado", TEAM_CARTOES);

            assertThrows(InvalidTicketStatusException.class,
                    () -> ticketService.closeTicket(rejected.getId()));
        }
    }

    @Nested
    @DisplayName("fluxo completo")
    class FullFlow {

        @Test
        @DisplayName("deve simular ciclo de abertura, fila, finalização e realocação")
        void shouldSimulateFullTicketLifecycle() {
            List<Ticket> activeCartoes = new ArrayList<>();
            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                activeCartoes.add(ticketService.assignTicket("conv-200-" + i, "Assunto " + i, TEAM_CARTOES));
            }

            Ticket queued = ticketService.assignTicket("conv-201", "Aguardando", TEAM_CARTOES);

            assertEquals(MAX_IN_SERVICE_PER_TEAM, ticketRepository.countByStatus(TicketStatus.IN_SERVICE));
            assertEquals(TicketStatus.QUEUED, queued.getStatus());

            ticketService.closeTicket(activeCartoes.get(0).getId());

            Ticket promoted = ticketRepository.findById(queued.getId()).orElseThrow();
            assertEquals(TicketStatus.IN_SERVICE, promoted.getStatus());
            assertEquals(MAX_IN_SERVICE_PER_TEAM, ticketRepository.countByStatus(TicketStatus.IN_SERVICE));
            assertEquals(0, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));

            Ticket newAssignment = ticketService.assignTicket("conv-202", "Novo chamado", TEAM_CARTOES);
            assertEquals(TicketStatus.QUEUED, newAssignment.getStatus());
        }
    }

    private void fillTeamToCapacity(Long teamId, String conversationPrefix) {
        for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
            ticketService.assignTicket(conversationPrefix + "-" + i, "Assunto " + i, teamId);
        }
        for (int i = 0; i < MAX_QUEUE_PER_TEAM; i++) {
            ticketService.assignTicket(conversationPrefix + "-queued-" + i, "Fila " + i, teamId);
        }
    }
}
