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
public class AgentDTO {
    private String id;
    private String name;
    private String avatar;
    private CapacityDTO capacity;
    private List<TicketDTO> tickets = new ArrayList<>();
}
