package com.example.campuscrush.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.example.campuscrush.dto.ChatMessageRequest;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.security.CampusUserDetails;
import com.example.campuscrush.service.MessageService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/confession/{confessionId}/send")
    public void sendMessage(
            @DestinationVariable Long confessionId,
            ChatMessageRequest request,
            Principal principal
    ) {
        CampusUserDetails userDetails = (CampusUserDetails) ((Authentication) principal).getPrincipal();
        User sender = userDetails.getUser();

        com.example.campuscrush.entity.message.Message savedMessage = 
            messageService.sendMessage(confessionId, sender, request.getContent());

        if (savedMessage != null) {
            // Broadcast a simple signal. Frontend will refetch messages via REST API.
            // sending Entity causes LazyInitializationException due to Jackson serialization of proxies.
            messagingTemplate.convertAndSend("/topic/confession/" + confessionId, "UPDATE");
        }
    }
}