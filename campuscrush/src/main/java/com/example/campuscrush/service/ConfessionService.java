package com.example.campuscrush.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.confession.ConfessionState;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.ConfessionRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfessionService {

    private final ConfessionRepository confessionRepository;
    private final com.example.campuscrush.repository.MessageRepository messageRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    @Transactional
    public Confession createInvitedConfession(User sender, String receiverRollNumber, String message) {
        if (confessionRepository.existsBySenderAndReceiverRollNumberAndState(
                sender, receiverRollNumber, ConfessionState.INVITED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "You've already sent an invite to this person. We'll notify you when they join.");
        }

        Confession confession = Confession.builder()
                .sender(sender)
                .receiverRollNumber(receiverRollNumber)
                .icebreakerMessage(message)
                .state(ConfessionState.INVITED)
                .receiverHasUnread(false)
                .build();

        return confessionRepository.save(confession);
    }

    @Transactional
    public void sendInviteEmail(Long confessionId, User sender) {
        Confession confession = confessionRepository.findById(confessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!confession.getSender().getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (confession.getState() != ConfessionState.INVITED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confession is not in invited state");
        }

        emailService.sendInvite(confession.getReceiverRollNumber().toLowerCase() + "@cuchd.in");
    }

    @Transactional
    public void resolveInvitedConfessions(User user) {
        List<Confession> pending = confessionRepository.findByReceiverRollNumberAndState(
                user.getRollNumber(), ConfessionState.INVITED);

        for (Confession c : pending) {
            c.setReceiver(user);
            c.setReceiverRollNumber(null);
            c.setState(ConfessionState.CREATED);
            c.setReceiverHasUnread(true);
            confessionRepository.save(c);
            try {
                emailService.sendConfessionNotification(user.getCollegeEmail());
            } catch (Exception e) {
                System.err.println("Failed to notify new user of waiting confession: " + e.getMessage());
            }
        }
    }

    @Transactional
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

        long recentCount = confessionRepository.countBySenderSince(
            sender, Instant.now().minusSeconds(3600)
        );
        if (recentCount >= 5) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS, "You can only send 5 confessions per hour"
            );
        }

        java.util.Optional<Confession> existing =
            confessionRepository.findFirstBySenderAndReceiverAndStateIn(
                sender,
                receiver,
                List.of(ConfessionState.CREATED, ConfessionState.UNLOCKED, ConfessionState.BLOCKED)
            );

        if (existing.isPresent()) {
            Confession confession = existing.get();
            com.example.campuscrush.entity.message.Message extra =
                com.example.campuscrush.entity.message.Message.builder()
                    .confession(confession)
                    .sender(sender)
                    .content(message)
                    .build();
            messageRepository.save(extra);
            confession.setSenderHasUnread(false);
            confession.setReceiverHasUnread(true);
            confessionRepository.save(confession);
            try {
                messagingTemplate.convertAndSendToUser(
                    receiver.getPublicId().toString(),
                    "/queue/confessions",
                    "NEW_MESSAGE"
                );
                messagingTemplate.convertAndSend(
                    "/topic/confession/" + confession.getId(),
                    "NEW_MESSAGE"
                );
            } catch (Exception e) {
                System.err.println("Failed to send WebSocket notification: " + e.getMessage());
            }
            return;
        }

        Confession confession = Confession.builder()
                .sender(sender)
                .receiver(receiver)
                .icebreakerMessage(message)
                .state(ConfessionState.CREATED)
                .receiverHasUnread(true) // New confession is unread by default
                .build();

        confessionRepository.save(confession);

        // Notify receiver via WebSocket (if online)
        try {
            messagingTemplate.convertAndSendToUser(
                receiver.getPublicId().toString(),
                "/queue/confessions",
                "NEW"
            );
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }

        // Notify receiver via email (for when they're offline)
        try {
            emailService.sendConfessionNotification(receiver.getCollegeEmail());
        } catch (Exception e) {
            System.err.println("Failed to send confession email: " + e.getMessage());
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

    public void blockConfession(Long confessionId, User user) {

        Confession confession = confessionRepository.findById(confessionId)
                .orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        boolean isParticipant =
                user.getId().equals(confession.getSender().getId()) ||
                user.getId().equals(confession.getReceiver().getId());

        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        confession.setState(ConfessionState.BLOCKED);
        confession.setBlockedBy(user);
        confessionRepository.save(confession);
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
    @Transactional(readOnly = true)
    public List<com.example.campuscrush.dto.ConfessionResponse> getConfessions(User user) {
        return confessionRepository.findAllByParticipant(user)
                .stream()
                .map(c -> {
                    boolean isSender = c.getSender().getId().equals(user.getId());

                    String aliasToShow;
                    java.util.UUID otherUserPublicId = null;

                    if (c.getState() == ConfessionState.INVITED) {
                        aliasToShow = c.getReceiverRollNumber();
                    } else {
                        User otherUser = isSender ? c.getReceiver() : c.getSender();
                        otherUserPublicId = otherUser.getPublicId();
                        if (isSender) {
                            aliasToShow = otherUser.getRollNumber() != null ? otherUser.getRollNumber() : otherUser.getPublicId().toString();
                        } else {
                            aliasToShow = otherUser.getDisplayAlias();
                        }
                    }

                    return com.example.campuscrush.dto.ConfessionResponse.builder()
                            .id(c.getId())
                            .otherUserAlias(aliasToShow)
                            .otherUserPublicId(otherUserPublicId)
                            .icebreakerMessage(c.getIcebreakerMessage())
                            .state(c.getState())
                            .createdAt(c.getCreatedAt())
                            .isSender(isSender)
                            .hasUnread(isSender ? Boolean.TRUE.equals(c.getSenderHasUnread()) : Boolean.TRUE.equals(c.getReceiverHasUnread()))
                            .isRevealed(Boolean.TRUE.equals(c.getIsRevealed()))
                            .build();
                })
                .toList();
    }

    @Transactional
    public void unblockConfession(Long confessionId, User user) {
        Confession confession = confessionRepository.findById(confessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (confession.getState() != ConfessionState.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confession is not blocked");
        }

        if (!user.getId().equals(confession.getBlockedBy().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the blocker can unblock");
        }

        confession.setState(ConfessionState.UNLOCKED);
        confession.setBlockedBy(null);
        confessionRepository.save(confession);
    }

    @Transactional(readOnly = true)
    public com.example.campuscrush.dto.ConfessionResponse getConfessionById(Long confessionId, User user) {
        Confession c = confessionRepository.findById(confessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isParticipant = user.getId().equals(c.getSender().getId()) ||
                                (c.getReceiver() != null && user.getId().equals(c.getReceiver().getId()));
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        boolean isSender = c.getSender().getId().equals(user.getId());

        String aliasToShow;
        java.util.UUID otherUserPublicId = null;

        if (c.getState() == ConfessionState.INVITED) {
            aliasToShow = c.getReceiverRollNumber();
        } else {
            User otherUser = isSender ? c.getReceiver() : c.getSender();
            otherUserPublicId = otherUser.getPublicId();
            aliasToShow = isSender
                    ? (otherUser.getRollNumber() != null ? otherUser.getRollNumber() : otherUser.getPublicId().toString())
                    : otherUser.getDisplayAlias();
        }

        return com.example.campuscrush.dto.ConfessionResponse.builder()
                .id(c.getId())
                .otherUserAlias(aliasToShow)
                .otherUserPublicId(otherUserPublicId)
                .icebreakerMessage(c.getIcebreakerMessage())
                .state(c.getState())
                .createdAt(c.getCreatedAt())
                .isSender(isSender)
                .hasUnread(isSender ? Boolean.TRUE.equals(c.getSenderHasUnread()) : Boolean.TRUE.equals(c.getReceiverHasUnread()))
                .isRevealed(Boolean.TRUE.equals(c.getIsRevealed()))
                .build();
    }

    @Transactional
    public void revealIdentity(Long confessionId, User sender) {
        Confession confession = confessionRepository.findById(confessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 1. Validate only Sender can reveal
        if (!confession.getSender().getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the sender can reveal identity");
        }

        // 2. Check if already revealed
        if (Boolean.TRUE.equals(confession.getIsRevealed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identity already revealed");
        }

        // 3. Update State
        confession.setIsRevealed(true);
        confessionRepository.save(confession);

        // 4. Create System Message
        String rollNumber = sender.getRollNumber() != null ? sender.getRollNumber() : sender.getPublicId().toString();
        String content = "Identity Revealed! 🎭 The sender is " + rollNumber;

        com.example.campuscrush.entity.message.Message systemArgs = com.example.campuscrush.entity.message.Message.builder()
                .confession(confession)
                .sender(sender)
                .content(content)
                .type(com.example.campuscrush.entity.message.MessageType.REVEAL)
                .build();
        
        messageRepository.save(systemArgs);

        // 5. Notify Socket
         try {
            // Notify Receiver
            messagingTemplate.convertAndSendToUser(
                confession.getReceiver().getPublicId().toString(),
                "/queue/confessions",
                "REVEALED"
            );
            
            // Notify Chat Topic
            messagingTemplate.convertAndSend(
                "/topic/confession/" + confession.getId(),
                "REVEALED"
            );
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }
    }
}