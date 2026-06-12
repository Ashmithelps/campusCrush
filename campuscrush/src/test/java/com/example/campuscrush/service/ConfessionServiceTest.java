package com.example.campuscrush.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.campuscrush.alias.AliasGenerator;
import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.confession.ConfessionState;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.error.LimitExceededException;
import com.example.campuscrush.repository.ConfessionRepository;
import com.example.campuscrush.repository.MessageRepository;

@ExtendWith(MockitoExtension.class)
class ConfessionServiceTest {

    @Mock ConfessionRepository confessionRepository;
    @Mock MessageRepository messageRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock EmailService emailService;

    private ConfessionService service;
    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        service = new ConfessionService(
            confessionRepository, messageRepository, messagingTemplate,
            emailService, new AliasGenerator());
        sender = user(1L, "23AAA00001");
        receiver = user(2L, "23BBB00002");
    }

    private User user(Long id, String roll) {
        User u = User.builder()
            .id(id)
            .collegeEmail(roll.toLowerCase() + "@cuchd.in")
            .rollNumber(roll)
            .build();
        u.setPublicId(UUID.randomUUID());
        return u;
    }

    /* ── Enforcement survives masks: block keys on the real account ── */

    @Test
    void blockedPairCannotOpenFreshThreadUnderNewMask() {
        when(confessionRepository.existsBlockedBetween(sender, receiver)).thenReturn(true);

        var result = service.processConfession(sender, receiver, "hello again");

        // Silent drop: looks like success, nothing is created or delivered
        assertEquals("CREATED", result.get("status"));
        verify(confessionRepository, never()).save(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void blockedThreadAppendIsSilentlyDropped() {
        Confession blocked = Confession.builder()
            .id(10L).sender(sender).receiver(receiver)
            .state(ConfessionState.BLOCKED).icebreakerMessage("hi").build();
        when(confessionRepository.findFirstBySenderAndReceiverAndStateIn(eq(sender), eq(receiver), any()))
            .thenReturn(Optional.of(blocked));

        service.createConfession(sender, receiver, "let me back in");

        verify(messageRepository, never()).save(any());
        verify(confessionRepository, never()).save(any());
    }

    /* ── Confession caps key on the real sender account ── */

    @Test
    void hourlyCapThrowsCodedException() {
        when(confessionRepository.findFirstBySenderAndReceiverAndStateIn(eq(sender), eq(receiver), any()))
            .thenReturn(Optional.empty());
        when(confessionRepository.countBySenderSince(eq(sender), any(Instant.class))).thenReturn(5L);

        LimitExceededException e = assertThrows(LimitExceededException.class,
            () -> service.createConfession(sender, receiver, "hi"));
        assertEquals(LimitExceededException.HOURLY_CAP, e.getCode());
        verify(confessionRepository, never()).save(any());
    }

    @Test
    void dailyDistinctRecipientCapCountsRegisteredAndInvited() {
        when(confessionRepository.findFirstBySenderAndReceiverAndStateIn(eq(sender), eq(receiver), any()))
            .thenReturn(Optional.empty());
        when(confessionRepository.countBySenderSince(eq(sender), any(Instant.class))).thenReturn(0L);
        when(confessionRepository.countDistinctReceiversSince(eq(sender), any(Instant.class))).thenReturn(2L);
        when(confessionRepository.countDistinctInvitedRollsSince(eq(sender), any(Instant.class))).thenReturn(1L);

        LimitExceededException e = assertThrows(LimitExceededException.class,
            () -> service.createConfession(sender, receiver, "hi"));
        assertEquals(LimitExceededException.DAILY_CAP, e.getCode());
        verify(confessionRepository, never()).save(any());
    }

    @Test
    void pendingCapBlocksWhenFiveConfessionsAwaitAnswers() {
        when(confessionRepository.findFirstBySenderAndReceiverAndStateIn(eq(sender), eq(receiver), any()))
            .thenReturn(Optional.empty());
        when(confessionRepository.countBySenderSince(eq(sender), any(Instant.class))).thenReturn(0L);
        when(confessionRepository.countBySenderAndStateIn(eq(sender), any())).thenReturn(5L);

        LimitExceededException e = assertThrows(LimitExceededException.class,
            () -> service.createConfession(sender, receiver, "hi"));
        assertEquals(LimitExceededException.PENDING_CAP, e.getCode());
        verify(confessionRepository, never()).save(any());
    }

    /* ── Mask generation: inbox-scoped uniqueness, campus-wide reuse ── */

    @Test
    void maskUniquenessIsCheckedOnlyAgainstReceiversInbox() {
        when(confessionRepository.findFirstBySenderAndReceiverAndStateIn(eq(sender), eq(receiver), any()))
            .thenReturn(Optional.empty());
        // Receiver's inbox already holds every bare mask — forces suffix retry
        when(confessionRepository.existsByReceiverAndSenderAlias(eq(receiver), anyString()))
            .thenAnswer(inv -> !inv.getArgument(1, String.class).endsWith(" 2"));

        service.createConfession(sender, receiver, "hi");

        ArgumentCaptor<Confession> saved = ArgumentCaptor.forClass(Confession.class);
        verify(confessionRepository, atLeastOnce()).save(saved.capture());
        String mask = saved.getValue().getSenderAlias();
        assertNotNull(mask);
        assertTrue(mask.endsWith(" 2"), "mask should have retried within the inbox scope: " + mask);
        // Scope is the receiver — never the sender, never campus-wide
        verify(confessionRepository, atLeastOnce()).existsByReceiverAndSenderAlias(eq(receiver), anyString());
        verify(confessionRepository, never()).existsByReceiverAndSenderAlias(eq(sender), anyString());
    }

    @Test
    void sameSenderGetsIndependentMasksPerConversation() {
        User receiver2 = user(3L, "23CCC00003");
        when(confessionRepository.findFirstBySenderAndReceiverAndStateIn(eq(sender), any(User.class), any()))
            .thenReturn(Optional.empty());

        service.createConfession(sender, receiver, "hi");
        service.createConfession(sender, receiver2, "hi");

        // Each generation consulted only its own viewer's inbox
        verify(confessionRepository, atLeastOnce()).existsByReceiverAndSenderAlias(eq(receiver), anyString());
        verify(confessionRepository, atLeastOnce()).existsByReceiverAndSenderAlias(eq(receiver2), anyString());
    }

    @Test
    void invitedMaskIsRegeneratedIfItCollidesWithNewUsersInbox() {
        User newUser = user(4L, "23DDD00004");
        Confession invited = Confession.builder()
            .id(20L).sender(sender).receiverRollNumber(newUser.getRollNumber())
            .state(ConfessionState.INVITED).icebreakerMessage("hi")
            .senderAlias("Velvet Tide").build();
        when(confessionRepository.findByReceiverRollNumberAndState(newUser.getRollNumber(), ConfessionState.INVITED))
            .thenReturn(List.of(invited));
        when(confessionRepository.existsByReceiverAndSenderAlias(eq(newUser), anyString()))
            .thenAnswer(inv -> "Velvet Tide".equals(inv.getArgument(1, String.class)));

        service.resolveInvitedConfessions(newUser);

        assertNotEquals("Velvet Tide", invited.getSenderAlias());
        assertNotNull(invited.getSenderAlias());
        assertEquals(ConfessionState.CREATED, invited.getState());
        assertEquals(newUser, invited.getReceiver());
    }

    @Test
    void revealInOneThreadDoesNotTouchOtherThreads() {
        Confession thread = Confession.builder()
            .id(30L).sender(sender).receiver(receiver)
            .state(ConfessionState.UNLOCKED).icebreakerMessage("hi")
            .senderAlias("Hollow Shore").isRevealed(false).build();
        when(confessionRepository.findById(30L)).thenReturn(Optional.of(thread));

        service.revealIdentity(30L, sender);

        // isRevealed is per-confession; the mask itself is untouched
        assertTrue(thread.getIsRevealed());
        assertEquals("Hollow Shore", thread.getSenderAlias());
        assertFalse(sender.getRollNumber().isEmpty());
    }
}
