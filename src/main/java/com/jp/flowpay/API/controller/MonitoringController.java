package com.jp.flowpay.API.controller;

import com.jp.flowpay.API.dto.ticketDTO.monitoring.RecentActivityDTO;
import com.jp.flowpay.API.service.MonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/monitoring")
    public ResponseEntity<List<RecentActivityDTO>> getRecentFinishedTickets() {
        return ResponseEntity.ok(monitoringService.getRecentFinishedTickets());
    }
}
