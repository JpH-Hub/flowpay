package com.jp.flowpay.API.exception;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(String teamName) {
        super("Team not found for subject: " + teamName);
    }
}
