package com.jp.flowpay.API.dto.ticketDTO.DashboardDTO;


import com.jp.flowpay.API.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TicketDTO {
    private String id;
    private String chatRef;
    private TicketStatus status;
}
