package com.jp.flowpay.API.dto.ticketDTO.response;

import com.jp.flowpay.API.enums.TicketStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloseTicketResponseDTO {
    private Long ticketId;

    private TicketStatus status;

}
