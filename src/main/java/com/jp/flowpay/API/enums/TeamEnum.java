package com.jp.flowpay.API.enums;

public enum TeamEnum {
    CREDIT_CARDS("Cartões"),
    LOANS("Empréstimos"),
    OTHERS("Outros Assuntos");

    private final String teamName;

    TeamEnum(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamName() {
        return teamName;
    }
}
