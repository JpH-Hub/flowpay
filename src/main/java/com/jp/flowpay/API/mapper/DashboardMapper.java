package com.jp.flowpay.API.mapper;

import com.jp.flowpay.API.dto.ticketDTO.DashboardDTO.*;
import com.jp.flowpay.API.entity.Agent;
import com.jp.flowpay.API.entity.Team;
import com.jp.flowpay.API.entity.Ticket;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashboardMapper {
    public TicketDTO toTicketDTO(Ticket ticket) {
        TicketDTO dto = new TicketDTO();
        dto.setId("ticket-" + ticket.getId());
        dto.setChatRef(ticket.getConversationRef());
        dto.setStatus(ticket.getStatus());
        return dto;
    }

    public QueueDTO toQueueDTO(List<Ticket> queuedTickets, int maxQueueSize) {
        QueueDTO dto = new QueueDTO();
        dto.setCurrent(queuedTickets.size());
        dto.setMax(maxQueueSize);

        List<TicketDTO> ticketDTOs = queuedTickets.stream()
                .map(this::toTicketDTO)
                .toList();

        dto.setTickets(ticketDTOs);
        return dto;
    }

    public AgentDTO toAgentDTO(Agent agent, List<Ticket> agentTickets, int maxActivePerAgent) {
        AgentDTO dto = new AgentDTO();
        dto.setId("agent-" + agent.getId());
        dto.setName(agent.getName());
        dto.setAvatar("/assets/avatars/agent-" + agent.getId() + ".png");

        List<TicketDTO> ticketDTOs = agentTickets.stream()
                .map(this::toTicketDTO)
                .toList();

        dto.setTickets(ticketDTOs);
        dto.setCapacity(new CapacityDTO(ticketDTOs.size(), maxActivePerAgent));

        return dto;
    }

    public MonitoringColumnDTO toMonitoringColumnDTO(Team team, List<AgentDTO> agentDTOs, QueueDTO queueDTO, int teamActiveTicketsCount, int totalTeamMaxCapacity) {
        MonitoringColumnDTO dto = new MonitoringColumnDTO();
        dto.setId(team.getName().toLowerCase().replace(" ", "-"));
        dto.setTitle(team.getName());
        dto.setAgents(agentDTOs);
        dto.setQueue(queueDTO);
        dto.setCapacity(new CapacityDTO(teamActiveTicketsCount, totalTeamMaxCapacity));
        return dto;
    }
}