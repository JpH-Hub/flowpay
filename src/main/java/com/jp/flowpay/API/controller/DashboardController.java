package com.jp.flowpay.API.controller;


import com.jp.flowpay.API.dto.ticketDTO.DashboardDTO.MonitoringColumnDTO;
import com.jp.flowpay.API.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<List<MonitoringColumnDTO>> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }

}
