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
            assertEquals(3, agentIds.size(), "Cada atendente deve receber no máximo um chamado");
        }

        @Test
        @DisplayName("deve colocar na fila quando todos os atendentes do time estiverem ocupados")
        void shouldQueueWhenTeamIsFull() {
            ticketService.assignTicket("conv-006", "A", TEAM_CARTOES);
            ticketService.assignTicket("conv-007", "B", TEAM_CARTOES);
            ticketService.assignTicket("conv-008", "C", TEAM_CARTOES);

            Ticket queued = ticketService.assignTicket("conv-009", "D", TEAM_CARTOES);

            assertEquals(TicketStatus.QUEUED, queued.getStatus());
            assertNull(queued.getAgentId());
            assertEquals(1, ticketRepository.countByStatus(TicketStatus.QUEUED));
        }

        @Test
        @DisplayName("deve rejeitar chamado quando a fila global estiver cheia")
        void shouldRejectWhenGlobalQueueIsFull() {
            fillTeamToCapacity(TEAM_CARTOES, "conv-010");
            fillTeamToCapacity(TEAM_EMPRESTIMOS, "conv-020");
            fillTeamToCapacity(TEAM_OUTROS, "conv-030");

            assertEquals(3, ticketRepository.countByStatus(TicketStatus.QUEUED));

            Ticket rejected = ticketService.assignTicket("conv-040", "Rejeitado", TEAM_CARTOES);

            assertEquals(TicketStatus.REJECTED, rejected.getStatus());
            assertNull(rejected.getAgentId());
            assertEquals(3, ticketRepository.countByStatus(TicketStatus.QUEUED));
        }

        @Test
        @DisplayName("deve permitir atendimento simultâneo em times diferentes")
        void shouldAllowConcurrentServiceAcrossTeams() {
            fillTeamToCapacity(TEAM_CARTOES, "conv-050");
            fillTeamToCapacity(TEAM_EMPRESTIMOS, "conv-060");
            fillTeamToCapacity(TEAM_OUTROS, "conv-070");

            assertEquals(9, ticketRepository.countByStatus(TicketStatus.IN_SERVICE));
            assertEquals(3, ticketRepository.countByStatus(TicketStatus.QUEUED));
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
            fillTeamToCapacity(TEAM_CARTOES, "conv-110");
            Ticket queued = ticketService.assignTicket("conv-114", "Na fila", TEAM_CARTOES);

            Ticket closed = ticketService.closeTicket(queued.getId());

            assertEquals(TicketStatus.CLOSED, closed.getStatus());
            assertEquals(1, ticketRepository.countByStatus(TicketStatus.QUEUED));
            assertEquals(3, ticketRepository.countByStatus(TicketStatus.IN_SERVICE));
        }

        @Test
        @DisplayName("deve promover o chamado mais antigo da fila do mesmo time")
        void shouldPromoteOldestQueuedTicketFromSameTeam() {
            Ticket first = ticketService.assignTicket("conv-120", "A", TEAM_CARTOES);
            ticketService.assignTicket("conv-121", "B", TEAM_CARTOES);
            ticketService.assignTicket("conv-122", "C", TEAM_CARTOES);
            Ticket oldestQueued = ticketService.assignTicket("conv-123", "D", TEAM_CARTOES);
            ticketService.assignTicket("conv-124", "E", TEAM_EMPRESTIMOS);
            ticketService.assignTicket("conv-125", "F", TEAM_EMPRESTIMOS);
            ticketService.assignTicket("conv-126", "G", TEAM_EMPRESTIMOS);
            Ticket emprestimosQueued = ticketService.assignTicket("conv-127", "H", TEAM_EMPRESTIMOS);

            ticketService.closeTicket(first.getId());

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
            ticketService.assignTicket("conv-133", "D", TEAM_CARTOES);

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
            fillTeamToCapacity(TEAM_EMPRESTIMOS, "conv-160");
            fillTeamToCapacity(TEAM_OUTROS, "conv-170");

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
            List<Ticket> activeCartoes = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) {
                activeCartoes.add(ticketService.assignTicket("conv-200-" + i, "Assunto " + i, TEAM_CARTOES));
            }

            Ticket queued = ticketService.assignTicket("conv-201", "Aguardando", TEAM_CARTOES);

            assertEquals(3, ticketRepository.countByStatus(TicketStatus.IN_SERVICE));
            assertEquals(TicketStatus.QUEUED, queued.getStatus());

            ticketService.closeTicket(activeCartoes.get(0).getId());

            Ticket promoted = ticketRepository.findById(queued.getId()).orElseThrow();
            assertEquals(TicketStatus.IN_SERVICE, promoted.getStatus());

            Ticket newAssignment = ticketService.assignTicket("conv-202", "Novo chamado", TEAM_CARTOES);
            assertEquals(TicketStatus.IN_SERVICE, newAssignment.getStatus());
        }
    }

    private void fillTeamToCapacity(Long teamId, String conversationPrefix) {
        for (int i = 0; i < 3; i++) {
            ticketService.assignTicket(conversationPrefix + "-" + i, "Assunto " + i, teamId);
        }
        ticketService.assignTicket(conversationPrefix + "-queued", "Fila", teamId);
    }
}
