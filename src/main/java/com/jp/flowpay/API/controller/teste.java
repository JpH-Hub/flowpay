package com.jp.flowpay.API.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class teste {
    @GetMapping("/hello")
    public String helloWorld() {
        return "Hello World!";
    }
}
