package com.jp.flowpay.API.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TicketService {

    @Value("${flowpay.ticket.max-queue-size:3}")
    private int maxQueueSize;

    @Value("${flowpay.ticket.max-active-per-agent:3}")
    private int maxActivePerAgent;

    private final TeamRepository teamRepository;
    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;
    private final TeamRoutingService teamRoutingService;

    public TicketService(TeamRepository teamRepository, AgentRepository agentRepository, TicketRepository ticketRepository, TeamRoutingService teamRoutingService) {
        this.teamRepository = teamRepository;
        this.agentRepository = agentRepository;
        this.ticketRepository = ticketRepository;
        this.teamRoutingService = teamRoutingService;
    }

    @Transactional
    public Ticket assignTicket(String conversationRef, String subject) {
        TeamEnum teamEnum = teamRoutingService.determineTeam(subject);
        Team team = teamRepository.findByNameIgnoreCase(teamEnum.getTeamName()).orElseThrow(() -> new TeamNotFoundException(teamEnum.getTeamName()));

        Optional<Agent> availableAgent = agentRepository.findAvailableByTeamId(team.getId(), maxActivePerAgent);

        if (availableAgent.isPresent()) {
            return ticketRepository.save(buildTicket(conversationRef, subject, TicketStatus.IN_SERVICE, team.getId(), availableAgent.get().getId()));
        }

        if (ticketRepository.countByStatusAndTeamId(TicketStatus.QUEUED, team.getId()) >= maxQueueSize) {
            return ticketRepository.save(buildTicket(conversationRef, subject, TicketStatus.REJECTED, team.getId(), null));
        }

        return ticketRepository.save(buildTicket(conversationRef, subject, TicketStatus.QUEUED, team.getId(), null));
    }

    @Transactional
    public Ticket closeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));

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
        Optional<Ticket> queuedTicket = ticketRepository.findOldestQueuedByTeamIdForUpdate(teamId);
        if (queuedTicket.isEmpty()) {
            return;
        }

        Optional<Agent> availableAgent = agentRepository.findAvailableByTeamId(teamId, maxActivePerAgent);
        if (availableAgent.isEmpty()) {
            return;
        }

        Ticket ticket = queuedTicket.get();
        ticket.setStatus(TicketStatus.IN_SERVICE);
        ticket.setAgentId(availableAgent.get().getId());
        ticketRepository.update(ticket);
    }

    private Ticket buildTicket(String conversationRef, String subject, TicketStatus status, Long teamId, Long agentId) {
        Ticket ticket = new Ticket();
        ticket.setConversationRef(conversationRef);
        ticket.setSubject(subject);
        ticket.setStatus(status);
        ticket.setTeamId(teamId);
        ticket.setAgentId(agentId);
        return ticket;
    }
}
