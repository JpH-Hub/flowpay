package com.jp.flowpay.API.dto.ticketDTO.DashboardDTO;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonitoringColumnDTO {
    private String id;
    private String title;
    private CapacityDTO capacity;
    private QueueDTO queue;
    private List<AgentDTO> agents = new ArrayList<>();
}
