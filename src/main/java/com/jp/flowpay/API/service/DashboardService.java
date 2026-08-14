package com.jp.flowpay.API.service;

import com.jp.flowpay.API.dto.ticketDTO.DashboardDTO.AgentDTO;
import com.jp.flowpay.API.dto.ticketDTO.DashboardDTO.MonitoringColumnDTO;
import com.jp.flowpay.API.dto.ticketDTO.DashboardDTO.QueueDTO;
import com.jp.flowpay.API.entity.Agent;
import com.jp.flowpay.API.entity.Team;
import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TicketStatus;
import com.jp.flowpay.API.mapper.DashboardMapper;
import com.jp.flowpay.API.repository.AgentRepository;
import com.jp.flowpay.API.repository.TeamRepository;
import com.jp.flowpay.API.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    @Value("${flowpay.ticket.max-queue-size:3}")
    private int maxQueueSize;

    @Value("${flowpay.ticket.max-active-per-agent:3}")
    private int maxActivePerAgent;

    private final TeamRepository teamRepository;
    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;
    private final DashboardMapper dashboardMapper;

    public DashboardService(TeamRepository teamRepository, AgentRepository agentRepository, TicketRepository ticketRepository, DashboardMapper dashboardMapper) {
        this.teamRepository = teamRepository;
        this.agentRepository = agentRepository;
        this.ticketRepository = ticketRepository;
        this.dashboardMapper = dashboardMapper;
    }

    public List<MonitoringColumnDTO> getDashboard() {
        List<MonitoringColumnDTO> columns = new ArrayList<>();
        List<Team> teams = teamRepository.findAll();

        for (Team team : teams) {
            List<Agent> agents = agentRepository.findByTeamId(team.getId());
            List<AgentDTO> agentDTOs = new ArrayList<>();
            int teamActiveTicketsCount = 0;

            for (Agent agent : agents) {
                List<Ticket> agentTickets = ticketRepository.findByAgentIdAndStatus(agent.getId(), TicketStatus.IN_SERVICE);

                AgentDTO agentDTO = dashboardMapper.toAgentDTO(agent, agentTickets, maxActivePerAgent);

                agentDTOs.add(agentDTO);
                teamActiveTicketsCount += agentTickets.size();
            }


            List<Ticket> queuedTickets = ticketRepository.findByTeamIdAndStatus(team.getId(), TicketStatus.QUEUED);

            QueueDTO queueDTO = dashboardMapper.toQueueDTO(queuedTickets, maxQueueSize);

            int totalTeamMaxCapacity = agents.size() * maxActivePerAgent;

            MonitoringColumnDTO column = dashboardMapper.toMonitoringColumnDTO(team, agentDTOs, queueDTO, teamActiveTicketsCount, totalTeamMaxCapacity);

            columns.add(column);
        }

        return columns;
    }
}