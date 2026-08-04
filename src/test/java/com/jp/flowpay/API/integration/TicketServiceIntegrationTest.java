package com.jp.flowpay.API.integration;

import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TicketStatus;
import com.jp.flowpay.API.exception.InvalidTicketStatusException;
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
class TicketServiceIntegrationTest {

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
        @DisplayName("deve classificar por regex e atribuir a um atendente de cartões")
        void shouldAssignToAvailableAgent() {
            Ticket savedTicket = ticketService.assignTicket("conv-001", "Problema no cartão");

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
        @DisplayName("deve rotear para Outros Assuntos quando não encontrar palavra-chave")
        void shouldRouteToOthersWhenNoKeywordMatch() {

            Ticket ticket = ticketService.assignTicket("conv-002", "Dúvida genérica");

            assertEquals(TEAM_OUTROS, ticket.getTeamId());
            assertEquals(TicketStatus.IN_SERVICE, ticket.getStatus());
        }

        @Test
        @DisplayName("deve distribuir chamados entre atendentes diferentes do mesmo time")
        void shouldAssignDifferentAgentsWithinTeam() {
            Ticket first = ticketService.assignTicket("conv-003", "Dúvida cartão 1");
            Ticket second = ticketService.assignTicket("conv-004", "Dúvida cartão 2");
            Ticket third = ticketService.assignTicket("conv-005", "Dúvida cartao 3");

            Set<Long> agentIds = Set.of(first.getAgentId(), second.getAgentId(), third.getAgentId());
            assertEquals(3, agentIds.size(), "Os três primeiros chamados devem ir para atendentes distintos");
        }

        @Test
        @DisplayName("deve permitir até 3 chamados simultâneos por atendente")
        void shouldAllowUpToThreeTicketsPerAgent() {
            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                Ticket ticket = ticketService.assignTicket("conv-006-" + i, "Problema cartão " + i);
                assertEquals(TicketStatus.IN_SERVICE, ticket.getStatus());
            }

            assertEquals(MAX_IN_SERVICE_PER_TEAM, ticketRepository.countByStatusAndTeamId(TicketStatus.IN_SERVICE, TEAM_CARTOES));

            Ticket queued = ticketService.assignTicket("conv-006-queued", "Fila cartão");
            assertEquals(TicketStatus.QUEUED, queued.getStatus());
        }

        @Test
        @DisplayName("deve colocar na fila quando todos os atendentes do time estiverem ocupados")
        void shouldQueueWhenTeamIsFull() {
            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                ticketService.assignTicket("conv-007-" + i, "Cartão " + i);
            }

            Ticket queued = ticketService.assignTicket("conv-009", "Mais um cartão");

            assertEquals(TicketStatus.QUEUED, queued.getStatus());
            assertNull(queued.getAgentId());
            assertEquals(1, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));
        }

        @Test
        @DisplayName("deve rejeitar chamado quando a fila do time estiver cheia")
        void shouldRejectWhenTeamQueueIsFull() {
            fillTeamToCapacity("cartão", "conv-010");

            assertEquals(MAX_QUEUE_PER_TEAM, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));

            Ticket rejected = ticketService.assignTicket("conv-040", "Cartão rejeitado");

            assertEquals(TicketStatus.REJECTED, rejected.getStatus());
            assertNull(rejected.getAgentId());
            assertEquals(MAX_QUEUE_PER_TEAM, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));
        }

        @Test
        @DisplayName("deve manter filas independentes entre times")
        void shouldKeepIndependentQueuesPerTeam() {

            fillTeamToCapacity("cartão", "conv-020");

            Ticket accepted = ticketService.assignTicket("conv-021", "Dúvida de Empréstimo");

            assertEquals(TicketStatus.IN_SERVICE, accepted.getStatus());
            assertEquals(TEAM_EMPRESTIMOS, accepted.getTeamId());
            assertEquals(MAX_QUEUE_PER_TEAM, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));
            assertEquals(0, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_EMPRESTIMOS));
        }

        @Test
        @DisplayName("deve permitir atendimento simultâneo em times diferentes")
        void shouldAllowConcurrentServiceAcrossTeams() {
            fillTeamToCapacity("cartão", "conv-050");
            fillTeamToCapacity("empréstimo", "conv-060");
            fillTeamToCapacity("outros", "conv-070");

            assertEquals(MAX_IN_SERVICE_PER_TEAM * 3, ticketRepository.countByStatus(TicketStatus.IN_SERVICE));
            assertEquals(MAX_QUEUE_PER_TEAM * 3, ticketRepository.countByStatus(TicketStatus.QUEUED));
        }
    }

    @Nested
    @DisplayName("closeTicket")
    class CloseTicket {

        @Test
        @DisplayName("deve finalizar chamado em atendimento")
        void shouldCloseInServiceTicket() {
            Ticket ticket = ticketService.assignTicket("conv-100", "Dúvida sobre cartão");

            Ticket closed = ticketService.closeTicket(ticket.getId());

            assertEquals(TicketStatus.CLOSED, closed.getStatus());
        }

        @Test
        @DisplayName("deve finalizar chamado na fila sem promover outros")
        void shouldCloseQueuedTicketWithoutPromotion() {
            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                ticketService.assignTicket("conv-110-" + i, "Cartão " + i);
            }
            Ticket queued = ticketService.assignTicket("conv-114", "Fila cartão");

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
                activeCartoes.add(ticketService.assignTicket("conv-120-" + i, "Cartão " + i));
            }
            Ticket oldestQueued = ticketService.assignTicket("conv-123", "Fila Cartão D");

            for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
                ticketService.assignTicket("conv-125-" + i, "Empréstimo " + i);
            }
            Ticket emprestimosQueued = ticketService.assignTicket("conv-127", "Fila Empréstimo H");


            ticketService.closeTicket(activeCartoes.get(0).getId());

            Ticket promotedCartoes = ticketRepository.findById(oldestQueued.getId()).orElseThrow();
            Ticket stillQueuedEmprestimos = ticketRepository.findById(emprestimosQueued.getId()).orElseThrow();


            assertEquals(TicketStatus.IN_SERVICE, promotedCartoes.getStatus());
            assertNotNull(promotedCartoes.getAgentId());

            assertEquals(TicketStatus.QUEUED, stillQueuedEmprestimos.getStatus());
        }

        @Test
        @DisplayName("deve liberar vaga e não colocar o próximo na fila")
        void shouldAcceptNewTicketAfterClosingOne() {
            Ticket first = ticketService.assignTicket("conv-130", "Cartão A");
            ticketService.assignTicket("conv-131", "Cartão B");
            ticketService.assignTicket("conv-132", "Cartão C");

            ticketService.closeTicket(first.getId());

            Ticket newTicket = ticketService.assignTicket("conv-134", "Cartão E");

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
            Ticket ticket = ticketService.assignTicket("conv-140", "Outros");
            ticketService.closeTicket(ticket.getId());

            assertThrows(InvalidTicketStatusException.class,
                    () -> ticketService.closeTicket(ticket.getId()));
        }

        @Test
        @DisplayName("deve lançar exceção ao fechar chamado rejeitado")
        void shouldThrowWhenClosingRejectedTicket() {
            fillTeamToCapacity("cartão", "conv-150");

            Ticket rejected = ticketService.assignTicket("conv-180", "Rejeitado cartão");

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
                activeCartoes.add(ticketService.assignTicket("conv-200-" + i, "Cartão " + i));
            }

            Ticket queued = ticketService.assignTicket("conv-201", "Aguardando cartão");

            assertEquals(MAX_IN_SERVICE_PER_TEAM, ticketRepository.countByStatusAndTeamId(TicketStatus.IN_SERVICE, TEAM_CARTOES));
            assertEquals(TicketStatus.QUEUED, queued.getStatus());


            ticketService.closeTicket(activeCartoes.get(0).getId());

            Ticket promoted = ticketRepository.findById(queued.getId()).orElseThrow();
            assertEquals(TicketStatus.IN_SERVICE, promoted.getStatus());
            assertEquals(MAX_IN_SERVICE_PER_TEAM, ticketRepository.countByStatusAndTeamId(TicketStatus.IN_SERVICE, TEAM_CARTOES));
            assertEquals(0, ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, TEAM_CARTOES));

            Ticket newAssignment = ticketService.assignTicket("conv-202", "Novo cartão");
            assertEquals(TicketStatus.QUEUED, newAssignment.getStatus());
        }
    }

    private void fillTeamToCapacity(String subjectKeyword, String conversationPrefix) {
        for (int i = 0; i < MAX_IN_SERVICE_PER_TEAM; i++) {
            ticketService.assignTicket(conversationPrefix + "-" + i, subjectKeyword + " " + i);
        }
        for (int i = 0; i < MAX_QUEUE_PER_TEAM; i++) {
            ticketService.assignTicket(conversationPrefix + "-queued-" + i, "Fila " + subjectKeyword + " " + i);
        }
    }
}