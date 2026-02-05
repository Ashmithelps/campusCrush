package com.example.campuscrush.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {


    @GetMapping("/public/hello")
    public String publicHello() {
        return "Public OK";
    }

    @GetMapping("/secure/hello")
    public String secureHello() {
        return "Secure OK";
    }
}

