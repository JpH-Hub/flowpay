package com.jp.flowpay.API.service;

import com.jp.flowpay.API.dto.ticketDTO.monitoring.RecentActivityDTO;
import com.jp.flowpay.API.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonitoringService {

    private static final int DEFAULT_RECENT_LIMIT = 20;

    private final TicketRepository ticketRepository;

    public MonitoringService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<RecentActivityDTO> getRecentFinishedTickets() {
        return ticketRepository.findRecentFinishedTickets(DEFAULT_RECENT_LIMIT);
    }
}
