package com.jp.flowpay.API.dto.ticketDTO.response;

import com.jp.flowpay.API.enums.TicketStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
public class TicketResponseDTO {
    private Long id;
    private String conversationRef;
    private String subject;
    private Long teamId;
    private TicketStatus status;
    private Long agentId;
    private LocalDateTime startedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
}
