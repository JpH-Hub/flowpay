package com.jp.flowpay.API.exception;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(Long teamId) {
        super("Team not found: " + teamId);
    }
}
