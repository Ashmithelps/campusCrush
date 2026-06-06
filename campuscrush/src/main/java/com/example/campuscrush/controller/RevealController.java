package com.example.campuscrush.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.campuscrush.dto.RevealKitRequest;
import com.example.campuscrush.dto.RevealStateResponse;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.security.util.SecurityUtils;
import com.example.campuscrush.service.RevealService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reveal")
@RequiredArgsConstructor
public class RevealController {

    private final RevealService revealService;

    @GetMapping("/kit")
    public RevealStateResponse getKit() {
        User user = SecurityUtils.currentUser();
        return revealService.getKit(user);
    }

    @PutMapping("/kit")
    public ResponseEntity<RevealStateResponse> updateKit(@RequestBody RevealKitRequest req) {
        User user = SecurityUtils.currentUser();
        RevealStateResponse resp = revealService.updateKit(user, req);
        return ResponseEntity.ok(resp);
    }
}
