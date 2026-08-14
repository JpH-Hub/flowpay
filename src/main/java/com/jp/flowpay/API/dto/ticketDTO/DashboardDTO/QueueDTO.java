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
public class QueueDTO {
    private int current;
    private int max;
    private List<TicketDTO> tickets = new ArrayList<>();
}
