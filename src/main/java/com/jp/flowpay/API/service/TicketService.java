package com.jp.flowpay.API.service;

import com.jp.flowpay.API.entity.Agent;
import com.jp.flowpay.API.entity.Team;
import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TicketStatus;
import com.jp.flowpay.API.exception.InvalidTicketStatusException;
import com.jp.flowpay.API.exception.TeamNotFoundException;
import com.jp.flowpay.API.exception.TicketNotFoundException;
import com.jp.flowpay.API.repository.AgentRepository;
import com.jp.flowpay.API.repository.TeamRepository;
import com.jp.flowpay.API.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TicketService {

    static final int MAX_QUEUE_SIZE = 3;

    private final TeamRepository teamRepository;
    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;

    public TicketService(TeamRepository teamRepository,
                         AgentRepository agentRepository,
                         TicketRepository ticketRepository) {
        this.teamRepository = teamRepository;
        this.agentRepository = agentRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public Ticket createTicket(String conversationRef, String subject) {
        Team team = teamRepository.findByNameIgnoreCase(subject)
                .orElseThrow(() -> new TeamNotFoundException(subject));

        Optional<Agent> availableAgent = agentRepository.findAvailableByTeamId(team.getId());

        if (availableAgent.isPresent()) {
            return ticketRepository.save(buildTicket(
                    conversationRef,
                    subject,
                    TicketStatus.IN_SERVICE,
                    team.getId(),
                    availableAgent.get().getId()
            ));
        }

        if (ticketRepository.countByStatus(TicketStatus.QUEUED) >= MAX_QUEUE_SIZE) {
            return ticketRepository.save(buildTicket(
                    conversationRef,
                    subject,
                    TicketStatus.REJECTED,
                    team.getId(),
                    null
            ));
        }

        return ticketRepository.save(buildTicket(
                conversationRef,
                subject,
                TicketStatus.QUEUED,
                team.getId(),
                null
        ));
    }

    @Transactional
    public Ticket closeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (ticket.getStatus() != TicketStatus.IN_SERVICE && ticket.getStatus() != TicketStatus.QUEUED) {
            throw new InvalidTicketStatusException(ticket.getStatus(), "close");
        }

        Long teamId = ticket.getTeamId();
        boolean wasInService = ticket.getStatus() == TicketStatus.IN_SERVICE;

        ticket.setStatus(TicketStatus.CLOSED);
        ticketRepository.update(ticket);

        if (wasInService) {
            promoteFromQueue(teamId);
        }

        return ticket;
    }

    private void promoteFromQueue(Long teamId) {
        Optional<Agent> availableAgent = agentRepository.findAvailableByTeamId(teamId);
        if (availableAgent.isEmpty()) {
            return;
        }

        Optional<Ticket> queuedTicket = ticketRepository.findOldestQueuedByTeamIdForUpdate(teamId);
        if (queuedTicket.isEmpty()) {
            return;
        }

        Ticket ticket = queuedTicket.get();
        ticket.setStatus(TicketStatus.IN_SERVICE);
        ticket.setAgentId(availableAgent.get().getId());
        ticketRepository.update(ticket);
    }

    private Ticket buildTicket(String conversationRef,
                               String subject,
                               TicketStatus status,
                               Long teamId,
                               Long agentId) {
        Ticket ticket = new Ticket();
        ticket.setConversationRef(conversationRef);
        ticket.setSubject(subject);
        ticket.setStatus(status);
        ticket.setTeamId(teamId);
        ticket.setAgentId(agentId);
        return ticket;
    }
}
