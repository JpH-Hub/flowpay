package com.jp.flowpay.API.entity;

import com.jp.flowpay.API.enums.TicketStatus;

import java.time.LocalDateTime;

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

    public Ticket(Long id,
                  String conversationRef,
                  String subject,
                  TicketStatus status,
                  Long teamId,
                  Long agentId,
                  LocalDateTime createdAt) {

        this.id = id;
        this.conversationRef = conversationRef;
        this.subject = subject;
        this.status = status;
        this.teamId = teamId;
        this.agentId = agentId;
        this.createdAt = createdAt;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getConversationRef() {
        return conversationRef;
    }

    public void setConversationRef(String conversationRef) {
        this.conversationRef = conversationRef;
    }


    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }


    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }


    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }


    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
