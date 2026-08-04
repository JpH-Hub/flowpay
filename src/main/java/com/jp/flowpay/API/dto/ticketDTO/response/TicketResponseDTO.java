package com.jp.flowpay.API.dto.ticketDTO.response;

import com.jp.flowpay.API.enums.TicketStatus;

public class TicketResponseDTO {
    private Long id;

    private String conversationRef;

    private String subject;

    private String team;

    private TicketStatus status;

    private Long agentId;

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

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
}
