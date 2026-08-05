package com.jp.flowpay.API.dto.ticketDTO.response;

import com.jp.flowpay.API.enums.TicketStatus;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TicketResponseDTO {
    private Long id;

    private String conversationRef;

    private String subject;

    private String team;

    private TicketStatus status;

    private Long agentId;

}
