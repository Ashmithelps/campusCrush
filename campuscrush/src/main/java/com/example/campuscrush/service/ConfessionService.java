package com.example.campuscrush.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.confession.ConfessionState;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.ConfessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfessionService {

    private final ConfessionRepository confessionRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public void createConfession(
            User sender,
            User receiver,
            String message
    ) {

        if (sender.getId().equals(receiver.getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "You cannot confess to yourself"
            );
        }

        boolean alreadySent =
            confessionRepository.existsBySenderAndReceiverAndStateIn(
                sender,
                receiver,
                List.of(ConfessionState.CREATED, ConfessionState.UNLOCKED, ConfessionState.BLOCKED)
            );

        if (alreadySent) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Confession already sent"
            );
        }

        Confession confession = Confession.builder()
                .sender(sender)
                .receiver(receiver)
                .icebreakerMessage(message)
                .state(ConfessionState.CREATED)
                .receiverHasUnread(true) // New confession is unread by default
                .build();

        confessionRepository.save(confession);

        // Notify Receiver
        try {
            messagingTemplate.convertAndSendToUser(
                receiver.getPublicId().toString(),
                "/queue/confessions",
                "NEW"
            );
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }
    }
    @Transactional
public void replyToConfession(Long confessionId, User receiver) {

    Confession confession = confessionRepository.findById(confessionId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND)
            );

    if (!confession.getReceiver().getId().equals(receiver.getId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    if (confession.getState() != ConfessionState.CREATED) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Cannot reply now"
        );
    }

    confession.setState(ConfessionState.UNLOCKED);
}
@Transactional
public void blockConfession(Long confessionId, User receiver) {

    Confession confession = confessionRepository.findById(confessionId)
            .orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND)
            );

    if (!confession.getReceiver().getId().equals(receiver.getId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    confession.setState(ConfessionState.BLOCKED);
}

@Transactional
public void markAsRead(Long confessionId, User user) {
    Confession confession = confessionRepository.findById(confessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (user.getId().equals(confession.getSender().getId())) {
        confession.setSenderHasUnread(false);
    } else if (user.getId().equals(confession.getReceiver().getId())) {
        confession.setReceiverHasUnread(false);
    }
    confessionRepository.save(confession);
}
    public List<com.example.campuscrush.dto.ConfessionResponse> getConfessions(User user) {
        return confessionRepository.findAllByParticipant(user)
                .stream()
                .map(c -> {
                    boolean isSender = c.getSender().getId().equals(user.getId());
                    User otherUser = isSender ? c.getReceiver() : c.getSender();
                    
                    String aliasToShow;
                    if (isSender) {
                        // If I sent it, I want to see WHO I sent it to (Roll Number)
                        aliasToShow = otherUser.getRollNumber() != null ? otherUser.getRollNumber() : otherUser.getPublicId().toString();
                    } else {
                        // If I received it, I see their Anonymous Alias
                        aliasToShow = otherUser.getDisplayAlias();
                    }

                    return com.example.campuscrush.dto.ConfessionResponse.builder()
                            .id(c.getId())
                            .otherUserAlias(aliasToShow)
                            .otherUserPublicId(otherUser.getPublicId())
                            .icebreakerMessage(c.getIcebreakerMessage())
                            .state(c.getState())
                            .createdAt(c.getCreatedAt())
                            .isSender(isSender)
                            .hasUnread(isSender ? Boolean.TRUE.equals(c.getSenderHasUnread()) : Boolean.TRUE.equals(c.getReceiverHasUnread()))
                            .build();
                })
                .toList();
    }
}