package com.jp.flowpay.API.unit;
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
import com.jp.flowpay.API.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private DashboardMapper dashboardMapper;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(dashboardService, "maxQueueSize", 3);
        ReflectionTestUtils.setField(dashboardService, "maxActivePerAgent", 3);
    }

    @Test
    void getDashboard_ShouldReturnEmptyList_WhenNoTeamsExist() {
        when(teamRepository.findAll()).thenReturn(Collections.emptyList());

        List<MonitoringColumnDTO> result = dashboardService.getDashboard();


        assertTrue(result.isEmpty());


        verify(agentRepository, never()).findByTeamId(any());
        verify(ticketRepository, never()).findByAgentIdAndStatus(any(), any());
    }

    @Test
    void getDashboard_ShouldReturnMappedColumns_WhenDataExists() {
        Team team = new Team();
        team.setId(1L);
        team.setName("Cartões");

        Agent agent = new Agent();
        agent.setId(10L);
        agent.setName("Atendente 1");

        Ticket inServiceTicket = new Ticket();
        inServiceTicket.setId(100L);
        inServiceTicket.setStatus(TicketStatus.IN_SERVICE);

        Ticket queuedTicket = new Ticket();
        queuedTicket.setId(200L);
        queuedTicket.setStatus(TicketStatus.QUEUED);

        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(agentRepository.findByTeamId(1L)).thenReturn(List.of(agent));

        when(ticketRepository.findByAgentIdAndStatus(10L, TicketStatus.IN_SERVICE))
                .thenReturn(List.of(inServiceTicket));

        when(ticketRepository.findByTeamIdAndStatus(1L, TicketStatus.QUEUED))
                .thenReturn(List.of(queuedTicket));


        AgentDTO fakeAgentDTO = new AgentDTO();
        QueueDTO fakeQueueDTO = new QueueDTO();
        MonitoringColumnDTO fakeColumnDTO = new MonitoringColumnDTO();


        when(dashboardMapper.toAgentDTO(eq(agent), eq(List.of(inServiceTicket)), eq(3)))
                .thenReturn(fakeAgentDTO);

        when(dashboardMapper.toQueueDTO(eq(List.of(queuedTicket)), eq(3)))
                .thenReturn(fakeQueueDTO);


        when(dashboardMapper.toMonitoringColumnDTO(eq(team), eq(List.of(fakeAgentDTO)), eq(fakeQueueDTO), eq(1), eq(3)))
                .thenReturn(fakeColumnDTO);

        List<MonitoringColumnDTO> result = dashboardService.getDashboard();

        assertEquals(1, result.size());
        assertEquals(fakeColumnDTO, result.get(0));

        verify(dashboardMapper).toMonitoringColumnDTO(team, List.of(fakeAgentDTO), fakeQueueDTO, 1, 3);
    }
}