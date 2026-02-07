package com.example.campuscrush.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.dto.MessageResponse;
import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.confession.ConfessionState;
import com.example.campuscrush.entity.message.Message;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.ConfessionRepository;
import com.example.campuscrush.repository.MessageRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConfessionRepository confessionRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    /* =========================
       SEND MESSAGE
       ========================= */
    @Transactional
    public Message sendMessage(Long confessionId, User sender, String content) {

        Confession confession = confessionRepository.findById(confessionId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Confession not found")
                );

        validateSendAccess(confession, sender);

        // Shadowban → silently drop
        if (sender.isShadowBanned()) {
            return null;
        }

        Message message = Message.builder()
                .confession(confession)
                .sender(sender)
                .content(content)
                .build();

        // 1. Set Unread Flag
        User notificationTarget;
        if (sender.getId().equals(confession.getSender().getId())) {
            confession.setReceiverHasUnread(true);
            notificationTarget = confession.getReceiver();
        } else {
            confession.setSenderHasUnread(true);
            notificationTarget = confession.getSender();
        }
        confessionRepository.save(confession);

        Message savedMessage = messageRepository.save(message);

        // 2. Notify Receiver (Real-time update)
        try {
            // Notify Dashboard (to update dot) and Chat (to show message)
            messagingTemplate.convertAndSendToUser(
                notificationTarget.getPublicId().toString(),
                "/queue/confessions", 
                "NEW_MESSAGE"
            );
            
            // Also notify the specific chat topic (if open)
            messagingTemplate.convertAndSend(
                "/topic/confession/" + confession.getId(),
                "NEW_MESSAGE"
            );
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }

        return savedMessage;
    }

    /* =========================
       GET MESSAGES
       ========================= */
    @Transactional
    public List<MessageResponse> getMessages(Long confessionId, User user) {

        Confession confession = confessionRepository.findById(confessionId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Confession not found")
                );

        validateReadAccess(confession, user);

        List<MessageResponse> messages = new java.util.ArrayList<>();

        // 1. Add Icebreaker as the first message
        messages.add(new MessageResponse(
            -1L, // Special ID for icebreaker
            confession.getSender().getId().equals(user.getId()) ? "SELF" : "OTHER",
            confession.getIcebreakerMessage(),
            confession.getCreatedAt()
        ));

        // 2. Add Database Messages
        List<MessageResponse> dbMessages = messageRepository
                .findByConfessionOrderBySentAtAsc(confession)
                .stream()
                .map(msg -> new MessageResponse(
                        msg.getId(),
                        msg.getSender().getId().equals(user.getId()) ? "SELF" : "OTHER",
                        msg.getContent(),
                        msg.getType(),
                        msg.getSentAt()
                ))
                .toList();
        
        messages.addAll(dbMessages);

        return messages;
    }

    /* =========================
       ACCESS VALIDATION
       ========================= */

    private void validateReadAccess(Confession confession, User currentUser) {

        boolean isParticipant =
                currentUser.getId().equals(confession.getSender().getId()) ||
                currentUser.getId().equals(confession.getReceiver().getId());

        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not part of this confession");
        }

        // CREATED → allowed to read (it will be empty list), so receiver can see UI
        if (confession.getState() == ConfessionState.CREATED) {
            return;
        }

        // BLOCKED → reading OLD messages is allowed
        // so no exception here
    }

    private void validateSendAccess(Confession confession, User currentUser) {

        boolean isParticipant =
                currentUser.getId().equals(confession.getSender().getId()) ||
                currentUser.getId().equals(confession.getReceiver().getId());

        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not part of this confession");
        }

        ConfessionState state = confession.getState();

        // CREATED → nobody can send
        if (state == ConfessionState.CREATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chat not unlocked yet");
        }

        // BLOCKED → only blocker can send (if you allow that)
        if (state == ConfessionState.BLOCKED) {

            if (confession.getBlockedBy() == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Blocked state without blocker"
                );
            }

            // blocked user cannot send
            if (!currentUser.getId().equals(confession.getBlockedBy().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are blocked from sending messages"
                );
            }
        }

        // UNLOCKED, REVEAL_PENDING, REVEALED → allowed
    }
}