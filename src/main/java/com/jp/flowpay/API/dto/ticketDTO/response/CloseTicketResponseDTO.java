package com.jp.flowpay.API.dto.ticketDTO.response;

import com.jp.flowpay.API.enums.TicketStatus;

public class CloseTicketResponseDTO {
    private Long ticketId;

    private TicketStatus status;

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }
}
