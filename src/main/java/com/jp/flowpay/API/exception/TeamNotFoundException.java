package com.jp.flowpay.API.exception;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(Long teamId) {
        super("Team not found: " + teamId);
    }

    public TeamNotFoundException(String teamName) {
        super("Team not found: " + teamName);
    }
}
