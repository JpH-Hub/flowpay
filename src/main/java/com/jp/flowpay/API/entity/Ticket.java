package com.jp.flowpay.API.entity;

import com.jp.flowpay.API.enums.TicketStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;



@Getter
@Setter
public class Ticket {
    private Long id;

    private String conversationRef;

    private String subject;

    private TicketStatus status;

    private Long teamId;

    private Long agentId;

    private LocalDateTime createdAt;

    private String teamName;

    public Ticket() {
    }

}