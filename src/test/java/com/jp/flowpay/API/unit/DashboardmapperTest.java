package com.jp.flowpay.API.unit;

import com.jp.flowpay.API.dto.ticketDTO.DashboardDTO.*;
import com.jp.flowpay.API.entity.Agent;
import com.jp.flowpay.API.entity.Team;
import com.jp.flowpay.API.entity.Ticket;
import com.jp.flowpay.API.enums.TicketStatus;
import com.jp.flowpay.API.mapper.DashboardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DashboardMapperTest {

    private DashboardMapper dashboardMapper;

    @BeforeEach
    void setUp() {

        dashboardMapper = new DashboardMapper();
    }

    @Test
    void toTicketDTO_ShouldMapCorrectly_AndAddPrefix() {
        Ticket ticket = new Ticket();
        ticket.setId(10L);
        ticket.setConversationRef("chat_abc123");
        ticket.setStatus(TicketStatus.IN_SERVICE);

        TicketDTO result = dashboardMapper.toTicketDTO(ticket);

        assertNotNull(result);
        assertEquals("ticket-10", result.getId());
        assertEquals("chat_abc123", result.getChatRef());
        assertEquals(TicketStatus.IN_SERVICE, result.getStatus());
    }

    @Test
    void toQueueDTO_ShouldMapTicketsAndCalculateCapacity() {
        Ticket t1 = new Ticket();
        t1.setId(1L);
        t1.setConversationRef("ref_1");
        t1.setStatus(TicketStatus.QUEUED);

        Ticket t2 = new Ticket();
        t2.setId(2L);
        t2.setConversationRef("ref_2");
        t2.setStatus(TicketStatus.QUEUED);

        List<Ticket> queuedTickets = List.of(t1, t2);
        int maxQueueSize = 5;

        QueueDTO result = dashboardMapper.toQueueDTO(queuedTickets, maxQueueSize);

        assertNotNull(result);
        assertEquals(2, result.getCurrent());
        assertEquals(5, result.getMax());
        assertEquals(2, result.getTickets().size());
        assertEquals("ticket-1", result.getTickets().get(0).getId());
    }

    @Test
    void toAgentDTO_ShouldMapCorrectly_FormatAvatarAndId() {
        Agent agent = new Agent();
        agent.setId(99L);
        agent.setName("João Silva");

        Ticket inServiceTicket = new Ticket();
        inServiceTicket.setId(5L);
        inServiceTicket.setStatus(TicketStatus.IN_SERVICE);

        List<Ticket> agentTickets = List.of(inServiceTicket);
        int maxActivePerAgent = 3;

        AgentDTO result = dashboardMapper.toAgentDTO(agent, agentTickets, maxActivePerAgent);

        assertNotNull(result);
        assertEquals("agent-99", result.getId());
        assertEquals("João Silva", result.getName());
        assertEquals("/assets/avatars/agent-99.png", result.getAvatar());

        assertNotNull(result.getCapacity());
        assertEquals(1, result.getCapacity().getCurrent());
        assertEquals(3, result.getCapacity().getMax());

        assertEquals(1, result.getTickets().size());
    }

    @Test
    void toMonitoringColumnDTO_ShouldMapCorrectly_AndFormatTeamId() {
        Team team = new Team();
        team.setName("Suporte Técnico");

        AgentDTO fakeAgent = new AgentDTO();
        List<AgentDTO> agents = List.of(fakeAgent);

        QueueDTO fakeQueue = new QueueDTO();
        int activeTickets = 4;
        int maxCapacity = 12;

        MonitoringColumnDTO result = dashboardMapper.toMonitoringColumnDTO(
                team, agents, fakeQueue, activeTickets, maxCapacity);

        assertNotNull(result);
        assertEquals("suporte-técnico", result.getId());
        assertEquals("Suporte Técnico", result.getTitle());

        assertEquals(1, result.getAgents().size());
        assertEquals(fakeQueue, result.getQueue());
        assertNotNull(result.getCapacity());
        assertEquals(4, result.getCapacity().getCurrent());
        assertEquals(12, result.getCapacity().getMax());
    }
}