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
    private final com.example.campuscrush.alias.AliasGenerator aliasGenerator;

    public static final int CONFESSIONS_PER_HOUR_LIMIT = 5;
    public static final int DAILY_DISTINCT_RECIPIENTS_LIMIT = 3;
    public static final int PENDING_CONFESSIONS_LIMIT = 5;

    // All caps key on the real sender account — masks are display-only
    private void enforceConfessionCaps(User sender) {
        long recentCount = confessionRepository.countBySenderSince(
            sender, Instant.now().minusSeconds(3600)
        );
        if (recentCount >= CONFESSIONS_PER_HOUR_LIMIT) {
            throw new com.example.campuscrush.error.LimitExceededException(
                com.example.campuscrush.error.LimitExceededException.HOURLY_CAP);
        }

        Instant dayAgo = Instant.now().minusSeconds(24 * 3600L);
        long distinctRecipients =
            confessionRepository.countDistinctReceiversSince(sender, dayAgo)
            + confessionRepository.countDistinctInvitedRollsSince(sender, dayAgo);
        if (distinctRecipients >= DAILY_DISTINCT_RECIPIENTS_LIMIT) {
            throw new com.example.campuscrush.error.LimitExceededException(
                com.example.campuscrush.error.LimitExceededException.DAILY_CAP);
        }

        long pending = confessionRepository.countBySenderAndStateIn(
            sender, List.of(ConfessionState.CREATED, ConfessionState.INVITED));
        if (pending >= PENDING_CONFESSIONS_LIMIT) {
            throw new com.example.campuscrush.error.LimitExceededException(
                com.example.campuscrush.error.LimitExceededException.PENDING_CAP);
        }
    }

    private void enforceMessageRateLimit(User sender) {
        long recentMessages = messageRepository.countBySenderSince(
            sender, Instant.now().minusSeconds(60)
        );
        if (recentMessages >= MessageService.MESSAGES_PER_MINUTE_LIMIT) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS, "You're sending messages too quickly. Slow down."
            );
        }
    }

    @Transactional
    public Confession createInvitedConfession(User sender, String receiverRollNumber, String message) {
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }
        if (message.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message too long (max 500 characters)");
        }
        if (confessionRepository.existsBySenderAndReceiverRollNumberAndState(
                sender, receiverRollNumber, ConfessionState.INVITED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "You've already sent an invite to this person. We'll notify you when they join.");
        }
        enforceConfessionCaps(sender);

        Confession confession = Confession.builder()
                .sender(sender)
                .receiverRollNumber(receiverRollNumber)
                .icebreakerMessage(message)
                .state(ConfessionState.INVITED)
                .receiverHasUnread(false)
                .senderAlias(aliasGenerator.generate(mask ->
                    confessionRepository.existsByReceiverRollNumberAndSenderAlias(receiverRollNumber, mask)))
                .build();

        return confessionRepository.save(confession);
    }

    @Transactional
    public java.util.Map<String, String> processConfession(User sender, User receiver, String message) {
        // Blocked pair (either direction) → silent drop, shadowban-style.
        // A blocked user must not reach the other party under a fresh mask,
        // and must not learn they were blocked.
        if (confessionRepository.existsBlockedBetween(sender, receiver)) {
            return java.util.Map.of("status", "CREATED");
        }

        // Check for mutual crush first
        java.util.Optional<Confession> reverse =
            confessionRepository.findFirstBySenderAndReceiverAndStateIn(
                receiver, sender,
                List.of(ConfessionState.CREATED, ConfessionState.UNLOCKED)
            );

        if (reverse.isPresent()) {
            Confession confession = reverse.get();
            triggerMutualReveal(confession);

            // Save sender's message into the chat
            if (message != null && !message.isBlank()) {
                com.example.campuscrush.entity.message.Message msg =
                    com.example.campuscrush.entity.message.Message.builder()
                        .confession(confession)
                        .sender(sender)
                        .content(message)
                        .build();
                messageRepository.save(msg);
            }

            return java.util.Map.of(
                "status", "MUTUAL",
                "confessionId", confession.getId().toString()
            );
        }

        createConfession(sender, receiver, message);
        return java.util.Map.of("status", "CREATED");
    }

    @Transactional
    public void triggerMutualReveal(Confession confession) {
        confession.setState(ConfessionState.REVEALED);
        confession.setIsRevealed(true);
        confession.setSenderHasUnread(true);
        confession.setReceiverHasUnread(true);
        confession.setMutualSeenBySender(false);
        confession.setMutualSeenByReceiver(false);
        confessionRepository.save(confession);

        com.example.campuscrush.entity.message.Message systemMsg =
            com.example.campuscrush.entity.message.Message.builder()
                .confession(confession)
                .sender(confession.getSender())
                .content("💕 It's mutual — you both had feelings for each other all along.")
                .type(com.example.campuscrush.entity.message.MessageType.MUTUAL)
                .build();
        messageRepository.save(systemMsg);

        try {
            String senderPub = confession.getSender().getPublicId().toString();
            String receiverPub = confession.getReceiver().getPublicId().toString();
            String chatDest = "/queue/confession/" + confession.getId();
            messagingTemplate.convertAndSendToUser(senderPub, "/queue/confessions", "MUTUAL");
            messagingTemplate.convertAndSendToUser(receiverPub, "/queue/confessions", "MUTUAL");
            messagingTemplate.convertAndSendToUser(senderPub, chatDest, "MUTUAL");
            messagingTemplate.convertAndSendToUser(receiverPub, chatDest, "MUTUAL");
        } catch (Exception e) {
            System.err.println("Failed to send mutual WebSocket notification: " + e.getMessage());
        }
    }

    @Transactional
    public void markMutualSeen(Long confessionId, User user) {
        Confession confession = confessionRepository.findById(confessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (confession.getSender().getId().equals(user.getId())) {
            confession.setMutualSeenBySender(true);
        } else if (confession.getReceiver() != null &&
                   confession.getReceiver().getId().equals(user.getId())) {
            confession.setMutualSeenByReceiver(true);
        }
        confessionRepository.save(confession);
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

        java.time.Instant now = java.time.Instant.now();

        // Per-sender cooldown: 7 days
        if (confession.getLastInviteSentAt() != null &&
                confession.getLastInviteSentAt().plusSeconds(7 * 24 * 3600L).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Invite already sent. You can resend after 7 days.");
        }

        // Global receiver cooldown: only one invite email per receiver every 3 days, regardless of who sends it
        confessionRepository.findLatestInviteSentAtForRollNumber(confession.getReceiverRollNumber())
            .ifPresent(lastSent -> {
                if (lastSent.plusSeconds(3 * 24 * 3600L).isAfter(now)) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "This person was recently invited. Please wait a few days before sending another invite.");
                }
            });

        confession.setLastInviteSentAt(now);
        confessionRepository.save(confession);
        emailService.sendInvite(confession.getReceiverRollNumber().toLowerCase() + "@cuchd.in");
    }

    @Transactional
    public void resolveInvitedConfessions(User user) {
        List<Confession> pending = confessionRepository.findByReceiverRollNumberAndState(
                user.getRollNumber(), ConfessionState.INVITED);

        // Masks were unique among this roll's invites; re-check against the
        // user's actual inbox now that they have one
        java.util.Set<String> assigned = new java.util.HashSet<>();
        for (Confession c : pending) {
            if (c.getSenderAlias() == null
                    || assigned.contains(c.getSenderAlias())
                    || confessionRepository.existsByReceiverAndSenderAlias(user, c.getSenderAlias())) {
                c.setSenderAlias(aliasGenerator.generate(mask ->
                    assigned.contains(mask)
                    || confessionRepository.existsByReceiverAndSenderAlias(user, mask)));
            }
            assigned.add(c.getSenderAlias());
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

        if (message != null && message.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message too long (max 500 characters)");
        }

        java.util.Optional<Confession> existing =
            confessionRepository.findFirstBySenderAndReceiverAndStateIn(
                sender,
                receiver,
                List.of(ConfessionState.CREATED, ConfessionState.UNLOCKED, ConfessionState.BLOCKED)
            );

        if (existing.isPresent()) {
            // Blocked thread → silent drop; the sender must not deliver
            // anything nor learn the thread is blocked.
            if (existing.get().getState() == ConfessionState.BLOCKED) {
                return;
            }
            // Duplicate confession lands as a chat message — limit it like one,
            // since it never increments the hourly confession count.
            enforceMessageRateLimit(sender);
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
                String receiverPub = receiver.getPublicId().toString();
                String senderPub = sender.getPublicId().toString();
                String chatDest = "/queue/confession/" + confession.getId();
                messagingTemplate.convertAndSendToUser(receiverPub, "/queue/confessions", "NEW_MESSAGE");
                messagingTemplate.convertAndSendToUser(receiverPub, chatDest, "NEW_MESSAGE");
                messagingTemplate.convertAndSendToUser(senderPub, chatDest, "NEW_MESSAGE");
            } catch (Exception e) {
                System.err.println("Failed to send WebSocket notification: " + e.getMessage());
            }
            return;
        }

        enforceConfessionCaps(sender);

        Confession confession = Confession.builder()
                .sender(sender)
                .receiver(receiver)
                .icebreakerMessage(message)
                .state(ConfessionState.CREATED)
                .receiverHasUnread(true) // New confession is unread by default
                .senderAlias(aliasGenerator.generate(mask ->
                    confessionRepository.existsByReceiverAndSenderAlias(receiver, mask)))
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
        confessionRepository.save(confession);
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
                        } else if (Boolean.TRUE.equals(c.getIsRevealed())) {
                            // Reveal resolves the mask in this thread only
                            aliasToShow = otherUser.getRollNumber() != null ? otherUser.getRollNumber() : otherUser.getPublicId().toString();
                        } else {
                            aliasToShow = c.getSenderAlias() != null ? c.getSenderAlias() : "Someone";
                        }
                    }

                    boolean isBlocker = c.getBlockedBy() != null
                            && user.getId().equals(c.getBlockedBy().getId());

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
                            .showMutualAnimation(isSender ? Boolean.FALSE.equals(c.getMutualSeenBySender()) : Boolean.FALSE.equals(c.getMutualSeenByReceiver()))
                            .isBlocker(isBlocker)
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
            if (isSender) {
                aliasToShow = otherUser.getRollNumber() != null ? otherUser.getRollNumber() : otherUser.getPublicId().toString();
            } else if (Boolean.TRUE.equals(c.getIsRevealed())) {
                aliasToShow = otherUser.getRollNumber() != null ? otherUser.getRollNumber() : otherUser.getPublicId().toString();
            } else {
                aliasToShow = c.getSenderAlias() != null ? c.getSenderAlias() : "Someone";
            }
        }

        boolean showMutual = isSender
                ? Boolean.FALSE.equals(c.getMutualSeenBySender())
                : Boolean.FALSE.equals(c.getMutualSeenByReceiver());

        boolean isBlocker = c.getBlockedBy() != null
                && user.getId().equals(c.getBlockedBy().getId());

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
                .showMutualAnimation(showMutual)
                .isBlocker(isBlocker)
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
        String content = "✦ The mask comes off — " + rollNumber;

        com.example.campuscrush.entity.message.Message systemArgs = com.example.campuscrush.entity.message.Message.builder()
                .confession(confession)
                .sender(sender)
                .content(content)
                .type(com.example.campuscrush.entity.message.MessageType.REVEAL)
                .build();
        
        messageRepository.save(systemArgs);

        // 5. Notify Socket
        try {
            String senderPub = sender.getPublicId().toString();
            String receiverPub = confession.getReceiver().getPublicId().toString();
            String chatDest = "/queue/confession/" + confession.getId();
            messagingTemplate.convertAndSendToUser(receiverPub, "/queue/confessions", "REVEALED");
            messagingTemplate.convertAndSendToUser(receiverPub, chatDest, "REVEALED");
            messagingTemplate.convertAndSendToUser(senderPub, chatDest, "REVEALED");
        } catch (Exception e) {
            System.err.println("Failed to send WebSocket notification: " + e.getMessage());
        }
    }
}