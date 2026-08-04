package com.jp.flowpay.API.dto.ticketDTO.request;

import jakarta.validation.constraints.NotBlank;

public class CreateTicketRequestDTO {
    @NotBlank
    private String conversationRef;

    @NotBlank
    private String subject;

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
}
