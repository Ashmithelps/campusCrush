package com.example.campuscrush.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.UserRepository;
import com.example.campuscrush.security.util.SecurityUtils;
import com.example.campuscrush.service.ConfessionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/confessions")
@RequiredArgsConstructor
public class ConfessionController {

    private final ConfessionService confessionService;
    private final UserRepository userRepository;

    @PostMapping("/{receiverId}")
    public void sendConfession(
            @PathVariable String receiverId,
            @RequestBody String message
    ) {
        User sender = SecurityUtils.currentUser();
        User receiver;

        try {
            UUID id = UUID.fromString(receiverId);
            receiver = userRepository.findByPublicId(id).orElse(null);
        } catch (IllegalArgumentException e) {
            receiver = null;
        }

        if (receiver == null) {
            // Roll numbers are stored in Uppercase
            receiver = userRepository.findByRollNumber(receiverId.toUpperCase())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        }

        confessionService.createConfession(sender, receiver, message);
    }
    @PostMapping("/{confessionId}/reply")
public void reply(@PathVariable Long confessionId) {
    User receiver = SecurityUtils.currentUser();
    confessionService.replyToConfession(confessionId, receiver);
}

@PostMapping("/{confessionId}/block")
public void block(@PathVariable Long confessionId) {
    User receiver = SecurityUtils.currentUser();
    confessionService.blockConfession(confessionId, receiver);
}

@PostMapping("/{confessionId}/reveal")
public void reveal(@PathVariable Long confessionId) {
    User sender = SecurityUtils.currentUser();
    confessionService.revealIdentity(confessionId, sender);
}

@PostMapping("/{confessionId}/read")
public void markRead(@PathVariable Long confessionId) {
    User user = SecurityUtils.currentUser();
    confessionService.markAsRead(confessionId, user);
}
    @org.springframework.web.bind.annotation.GetMapping
    public java.util.List<com.example.campuscrush.dto.ConfessionResponse> getMyConfessions() {
        User user = SecurityUtils.currentUser();
        return confessionService.getConfessions(user);
    }
}