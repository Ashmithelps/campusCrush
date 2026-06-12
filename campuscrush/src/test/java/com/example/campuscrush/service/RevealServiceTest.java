package com.example.campuscrush.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.dto.GuessRequest;
import com.example.campuscrush.dto.GuessResponse;
import com.example.campuscrush.dto.RevealKitRequest;
import com.example.campuscrush.dto.RevealStateResponse;
import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.confession.ConfessionState;
import com.example.campuscrush.entity.message.Message;
import com.example.campuscrush.entity.message.MessageType;
import com.example.campuscrush.entity.reveal.RevealKit;
import com.example.campuscrush.entity.reveal.RevealState;
import com.example.campuscrush.entity.reveal.RevealStatus;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.ConfessionRepository;
import com.example.campuscrush.repository.MessageRepository;
import com.example.campuscrush.repository.RevealGuessRepository;
import com.example.campuscrush.repository.RevealKitRepository;
import com.example.campuscrush.repository.RevealStateRepository;

@ExtendWith(MockitoExtension.class)
class RevealServiceTest {

    @Mock RevealKitRepository kitRepo;
    @Mock RevealStateRepository stateRepo;
    @Mock RevealGuessRepository guessRepo;
    @Mock ConfessionRepository confessionRepo;
    @Mock MessageRepository messageRepo;
    @Mock SimpMessagingTemplate messaging;

    private RevealService service;
    private User sender;
    private User receiver;
    private Confession thread;

    @BeforeEach
    void setUp() {
        service = new RevealService(kitRepo, stateRepo, guessRepo, confessionRepo, messageRepo, messaging);
        sender = user(1L, "23AAA00001");
        receiver = user(2L, "23BBB00002");
        thread = Confession.builder()
            .id(100L).sender(sender).receiver(receiver)
            .state(ConfessionState.UNLOCKED).icebreakerMessage("hi")
            .senderAlias("Velvet Tide").isRevealed(false).build();
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

    private RevealKit completeKit(Confession c) {
        return RevealKit.builder()
            .id(7L).confession(c)
            .hint1("Plays guitar").hint2("Lives in Tower B").hint3("Always late")
            .guessingEnabled(true).build();
    }

    private GuessResponse guess(String roll) {
        return service.submitGuess(thread.getId(), receiver, new GuessRequest(roll));
    }

    /* ── Kit is per-conversation ── */

    @Test
    void kitBindsToTheConversationNotTheUser() {
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.empty());

        service.updateKit(sender, 100L, new RevealKitRequest("a", "b", "c", true));

        ArgumentCaptor<RevealKit> saved = ArgumentCaptor.forClass(RevealKit.class);
        verify(kitRepo).save(saved.capture());
        assertEquals(thread, saved.getValue().getConfession());
        assertTrue(saved.getValue().isGuessingEnabled());
    }

    @Test
    void onlyTheSenderCanSetHints() {
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> service.updateKit(receiver, 100L, new RevealKitRequest("a", "b", "c", true)));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        verify(kitRepo, never()).save(any());
    }

    @Test
    void hintsInOneThreadDoNotEnableGuessingInAnother() {
        // Same sender, second thread, no kit set there
        Confession other = Confession.builder()
            .id(200L).sender(sender).receiver(receiver)
            .state(ConfessionState.UNLOCKED).icebreakerMessage("hey")
            .senderAlias("Hollow Shore").isRevealed(false).build();
        when(confessionRepo.findById(200L)).thenReturn(Optional.of(other));
        when(kitRepo.findByConfession(other)).thenReturn(Optional.empty());
        when(stateRepo.findByConfession(other)).thenReturn(Optional.empty());

        RevealStateResponse resp = service.getRevealState(200L, receiver);

        assertFalse(resp.canGuess());
        assertTrue(resp.hints().isEmpty());
    }

    /* ── Hint progression and lockout ── */

    @Test
    void wrongGuessUnlocksNextHintAndBurnsATry() {
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.of(completeKit(thread)));
        when(stateRepo.findByConfession(thread)).thenReturn(Optional.empty()); // fresh round: 3 tries, hint 1

        GuessResponse resp = guess("23ZZZ09999");

        assertFalse(resp.correct());
        assertEquals(2, resp.guessesRemaining());
        assertEquals(2, resp.hintsUnlocked());
        assertEquals(RevealStatus.HIDDEN.name(), resp.status());
    }

    @Test
    void hintsNeverUnlockPastThree() {
        RevealState state = RevealState.builder()
            .confession(thread).guessesRemaining(2).hintsUnlocked(3).build();
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.of(completeKit(thread)));
        when(stateRepo.findByConfession(thread)).thenReturn(Optional.of(state));

        GuessResponse resp = guess("23ZZZ09999");

        assertEquals(3, resp.hintsUnlocked());
        assertEquals(1, resp.guessesRemaining());
    }

    @Test
    void thirdWrongGuessLocksForTwentyFourHours() {
        RevealState state = RevealState.builder()
            .confession(thread).guessesRemaining(1).hintsUnlocked(3).build();
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.of(completeKit(thread)));
        when(stateRepo.findByConfession(thread)).thenReturn(Optional.of(state));

        GuessResponse resp = guess("23ZZZ09999");

        assertEquals(0, resp.guessesRemaining());
        assertNotNull(state.getLockedUntil());
        Instant expected = Instant.now().plus(24, ChronoUnit.HOURS);
        assertTrue(Math.abs(state.getLockedUntil().getEpochSecond() - expected.getEpochSecond()) < 60);
    }

    @Test
    void guessingWhileLockedReturns429() {
        RevealState state = RevealState.builder()
            .confession(thread).guessesRemaining(0).hintsUnlocked(3)
            .lockedUntil(Instant.now().plus(1, ChronoUnit.HOURS)).build();
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.of(completeKit(thread)));
        when(stateRepo.findByConfession(thread)).thenReturn(Optional.of(state));

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> guess("23ZZZ09999"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
        verify(guessRepo, never()).save(any());
    }

    @Test
    void expiredLockResetsTheRound() {
        RevealState state = RevealState.builder()
            .confession(thread).guessesRemaining(0).hintsUnlocked(3)
            .lockedUntil(Instant.now().minus(1, ChronoUnit.HOURS)).build();
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.of(completeKit(thread)));
        when(stateRepo.findByConfession(thread)).thenReturn(Optional.of(state));

        GuessResponse resp = guess("23ZZZ09999");

        // Round reset to 3 tries / hint 1, then this wrong guess burned one
        assertEquals(2, resp.guessesRemaining());
        assertEquals(2, resp.hintsUnlocked());
        assertNull(state.getLockedUntil());
    }

    /* ── Correct guess reveals this thread only ── */

    @Test
    void correctGuessRevealsAndDropsTheMask() {
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.of(completeKit(thread)));
        when(stateRepo.findByConfession(thread)).thenReturn(Optional.empty());

        GuessResponse resp = guess(sender.getRollNumber().toLowerCase()); // case-insensitive

        assertTrue(resp.correct());
        assertEquals(RevealStatus.GUESSED.name(), resp.status());
        assertTrue(thread.getIsRevealed());

        ArgumentCaptor<Message> msg = ArgumentCaptor.forClass(Message.class);
        verify(messageRepo).save(msg.capture());
        assertEquals(MessageType.REVEAL, msg.getValue().getType());
        assertTrue(msg.getValue().getContent().contains(sender.getRollNumber()));
    }

    @Test
    void guessIsValidatedOnlyAgainstThisThreadsSender() {
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.of(completeKit(thread)));
        when(stateRepo.findByConfession(thread)).thenReturn(Optional.empty());

        // A valid roll of some other real person is still just "wrong" here
        GuessResponse resp = guess(receiver.getRollNumber());

        assertFalse(resp.correct());
        assertFalse(Boolean.TRUE.equals(thread.getIsRevealed()));
    }

    /* ── Gating and the no-leak contract ── */

    @Test
    void receiverCannotGuessUntilKitCompleteAndEnabled() {
        RevealKit incomplete = RevealKit.builder()
            .confession(thread).hint1("a").hint2("b").guessingEnabled(true).build();
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.of(incomplete));
        when(stateRepo.findByConfession(thread)).thenReturn(Optional.empty());

        RevealStateResponse resp = service.getRevealState(100L, receiver);

        assertFalse(resp.canGuess());
        assertTrue(resp.hints().isEmpty());

        assertThrows(ResponseStatusException.class, () -> guess("23ZZZ09999"));
    }

    @Test
    void receiverResponseLeaksNothingOfTheSendersView() {
        RevealState state = RevealState.builder()
            .confession(thread).guessesRemaining(2).hintsUnlocked(2).build();
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));
        when(kitRepo.findByConfession(thread)).thenReturn(Optional.of(completeKit(thread)));
        when(stateRepo.findByConfession(thread)).thenReturn(Optional.of(state));

        RevealStateResponse resp = service.getRevealState(100L, receiver);

        assertEquals("RECEIVER", resp.role());
        assertTrue(resp.canGuess());
        // Only the unlocked hints, in order — never the full kit
        assertEquals(List.of("Plays guitar", "Lives in Tower B"), resp.hints());
        // Sender-only fields must be null
        assertNull(resp.guessingEnabled());
        assertNull(resp.kitComplete());
        assertNull(resp.hint1());
        assertNull(resp.hint2());
        assertNull(resp.hint3());
        assertNull(resp.guesses());
    }

    @Test
    void outsiderCannotReadRevealState() {
        User stranger = user(9L, "23XXX00009");
        when(confessionRepo.findById(100L)).thenReturn(Optional.of(thread));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> service.getRevealState(100L, stranger));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
}
