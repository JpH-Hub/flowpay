package com.jp.flowpay.API.controller;

import com.jp.flowpay.API.exception.InvalidTicketStatusException;
import com.jp.flowpay.API.exception.TeamNotFoundException;
import com.jp.flowpay.API.exception.TicketNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTeamNotFound(TeamNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTicketNotFound(TicketNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTicketStatusException.class)
    public ResponseEntity<Map<String, String>> handleInvalidStatus(InvalidTicketStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

}
