package com.jp.flowpay.API.exception;

import com.jp.flowpay.API.enums.TicketStatus;

public class InvalidTicketStatusException extends RuntimeException {

    public InvalidTicketStatusException(TicketStatus currentStatus, String operation) {
        super("Cannot " + operation + " ticket with status: " + currentStatus);
    }
}
