package com.example.campuscrush.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.dto.GuessRequest;
import com.example.campuscrush.dto.GuessResponse;
import com.example.campuscrush.dto.RevealStateResponse;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.UserRepository;
import com.example.campuscrush.security.util.SecurityUtils;
import com.example.campuscrush.service.ConfessionService;
import com.example.campuscrush.service.RevealService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/confessions")
@RequiredArgsConstructor
public class ConfessionController {

    private final ConfessionService confessionService;
    private final RevealService revealService;
    private final UserRepository userRepository;

    private static final java.util.regex.Pattern ROLL_OR_UUID =
        java.util.regex.Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$" +
            "|^[A-Za-z0-9]{4,20}$"
        );

    @PostMapping("/{receiverId}")
    public java.util.Map<String, String> sendConfession(
            @PathVariable String receiverId,
            @RequestBody String message
    ) {
        if (!ROLL_OR_UUID.matcher(receiverId).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid recipient identifier");
        }
        User sender = SecurityUtils.currentUser();
        User receiver = null;

        try {
            UUID id = UUID.fromString(receiverId);
            receiver = userRepository.findByPublicId(id).orElse(null);
        } catch (IllegalArgumentException e) {
            // not a UUID — fall through to roll number lookup
        }

        if (receiver == null) {
            receiver = userRepository.findByRollNumber(receiverId.toUpperCase()).orElse(null);
        }

        if (receiver != null) {
            return confessionService.processConfession(sender, receiver, message);
        } else {
            String rollNumber = receiverId.toUpperCase();
            com.example.campuscrush.entity.confession.Confession saved =
                confessionService.createInvitedConfession(sender, rollNumber, message);
            return java.util.Map.of(
                "status", "INVITED",
                "confessionId", saved.getId().toString(),
                "invitedEmail", rollNumber.toLowerCase() + "@cuchd.in"
            );
        }
    }
    @PostMapping("/{confessionId}/mutual-seen")
    public void markMutualSeen(@PathVariable Long confessionId) {
        User user = SecurityUtils.currentUser();
        confessionService.markMutualSeen(confessionId, user);
    }

    @PostMapping("/{confessionId}/invite")
    public void sendInvite(@PathVariable Long confessionId) {
        User sender = SecurityUtils.currentUser();
        confessionService.sendInviteEmail(confessionId, sender);
    }

    @PostMapping("/{confessionId}/reply")
public void reply(@PathVariable Long confessionId) {
    User receiver = SecurityUtils.currentUser();
    confessionService.replyToConfession(confessionId, receiver);
}

@PostMapping("/{confessionId}/block")
public void block(@PathVariable Long confessionId) {
    User user = SecurityUtils.currentUser();
    confessionService.blockConfession(confessionId, user);
}

@PostMapping("/{confessionId}/unblock")
public void unblock(@PathVariable Long confessionId) {
    User user = SecurityUtils.currentUser();
    confessionService.unblockConfession(confessionId, user);
}

@GetMapping("/{confessionId}")
public com.example.campuscrush.dto.ConfessionResponse getConfession(@PathVariable Long confessionId) {
    User user = SecurityUtils.currentUser();
    return confessionService.getConfessionById(confessionId, user);
}

@PostMapping("/{confessionId}/reveal")
public void reveal(@PathVariable Long confessionId) {
    User sender = SecurityUtils.currentUser();
    confessionService.revealIdentity(confessionId, sender);
    revealService.onManualReveal(confessionId);
}

@GetMapping("/{confessionId}/reveal-state")
public RevealStateResponse getRevealState(@PathVariable Long confessionId) {
    User caller = SecurityUtils.currentUser();
    return revealService.getRevealState(confessionId, caller);
}

@PostMapping("/{confessionId}/guess")
public GuessResponse submitGuess(
        @PathVariable Long confessionId,
        @RequestBody GuessRequest req) {
    User guesser = SecurityUtils.currentUser();
    return revealService.submitGuess(confessionId, guesser, req);
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