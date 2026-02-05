package com.example.campuscrush.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.campuscrush.dto.MessageResponse;

import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.security.util.SecurityUtils;
import com.example.campuscrush.service.MessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/{confessionId}")
    public void sendMessage(
            @PathVariable Long confessionId,
            @RequestBody String content
    ) {
        User sender = SecurityUtils.currentUser();
        messageService.sendMessage(confessionId, sender, content);
    }

    @GetMapping("/{confessionId}")
public List<MessageResponse> getMessages(
        @PathVariable Long confessionId
) {
    User user = SecurityUtils.currentUser();
    return messageService.getMessages(confessionId, user);
}
}