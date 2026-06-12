package com.example.campuscrush.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.campuscrush.dto.MeResponse;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.security.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    @GetMapping
    public MeResponse me() {
        User user = SecurityUtils.currentUser();
        return new MeResponse(user.getPublicId(), user.getRollNumber());
    }
}
