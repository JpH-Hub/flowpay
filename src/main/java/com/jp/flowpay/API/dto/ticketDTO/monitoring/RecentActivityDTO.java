package com.jp.flowpay.API.dto.ticketDTO.monitoring;

import com.jp.flowpay.API.enums.TicketStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RecentActivityDTO {
    private Long id;
    private String chatRef;
    private String subject;
    private TicketStatus status;
    private String agentName;
    private String teamName;
    private LocalDateTime closedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
}
