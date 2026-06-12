package com.example.campuscrush.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PutMapping("/kit/{confessionId}")
    public ResponseEntity<RevealStateResponse> updateKit(
            @PathVariable Long confessionId,
            @RequestBody RevealKitRequest req) {
        User user = SecurityUtils.currentUser();
        RevealStateResponse resp = revealService.updateKit(user, confessionId, req);
        return ResponseEntity.ok(resp);
    }
}
