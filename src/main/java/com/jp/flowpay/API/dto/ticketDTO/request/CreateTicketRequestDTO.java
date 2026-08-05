package com.jp.flowpay.API.dto.ticketDTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CreateTicketRequestDTO {
    @NotBlank
    private String conversationRef;

    @NotBlank
    private String subject;
}
