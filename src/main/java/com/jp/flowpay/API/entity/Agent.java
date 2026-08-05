package com.jp.flowpay.API.entity;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Agent {
    private Long id;

    private String name;

    private Long teamId;


    public Agent() {
    }

    public Agent(Long id, String name, Long teamId) {
        this.id = id;
        this.name = name;
        this.teamId = teamId;
    }
}
