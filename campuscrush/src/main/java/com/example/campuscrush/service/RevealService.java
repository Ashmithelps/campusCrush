package com.example.campuscrush.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.campuscrush.dto.GuessRequest;
import com.example.campuscrush.dto.GuessResponse;
import com.example.campuscrush.dto.RevealKitRequest;
import com.example.campuscrush.dto.RevealStateResponse;
import com.example.campuscrush.entity.confession.Confession;
import com.example.campuscrush.entity.confession.ConfessionState;
import com.example.campuscrush.entity.message.Message;
import com.example.campuscrush.entity.message.MessageType;
import com.example.campuscrush.entity.reveal.RevealGuess;
import com.example.campuscrush.entity.reveal.RevealKit;
import com.example.campuscrush.entity.reveal.RevealState;
import com.example.campuscrush.entity.reveal.RevealStatus;
import com.example.campuscrush.entity.user.User;
import com.example.campuscrush.repository.ConfessionRepository;
import com.example.campuscrush.repository.MessageRepository;
import com.example.campuscrush.repository.RevealGuessRepository;
import com.example.campuscrush.repository.RevealKitRepository;
import com.example.campuscrush.repository.RevealStateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevealService {

    private static final Pattern ROLL_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,20}$");
    private static final int MAX_GUESSES  = 3;
    private static final int LOCK_HOURS   = 24;
    private static final Set<String> BLOCKED_WORDS = Set.of(
        "fuck", "shit", "bitch", "asshole", "bastard", "cunt", "dick"
    );

    private final RevealKitRepository   kitRepo;
    private final RevealStateRepository stateRepo;
    private final RevealGuessRepository guessRepo;
    private final ConfessionRepository  confessionRepo;
    private final MessageRepository     messageRepo;
    private final SimpMessagingTemplate messaging;

    // ── Kit management ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RevealStateResponse getKit(User user) {
        RevealKit kit = kitRepo.findByUser(user).orElse(null);
        return senderView(null, kit, List.of());
    }

    @Transactional
    public RevealStateResponse updateKit(User user, RevealKitRequest req) {
        validateHint(req.hint1());
        validateHint(req.hint2());
        validateHint(req.hint3());

        RevealKit kit = kitRepo.findByUser(user)
            .orElseGet(() -> RevealKit.builder().user(user).build());

        kit.setHint1(trimOrNull(req.hint1()));
        kit.setHint2(trimOrNull(req.hint2()));
        kit.setHint3(trimOrNull(req.hint3()));
        kit.setGuessingEnabled(req.guessingEnabled());
        kit.setUpdatedAt(Instant.now());
        kitRepo.save(kit);

        return senderView(null, kit, List.of());
    }

    // ── Reveal state ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RevealStateResponse getRevealState(Long confessionId, User caller) {
        Confession confession = confessionRepo.findById(confessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isSender   = confession.getSender().getId().equals(caller.getId());
        boolean isReceiver = confession.getReceiver() != null
            && confession.getReceiver().getId().equals(caller.getId());

        if (!isSender && !isReceiver) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        RevealState state = stateRepo.findByConfession(confession).orElse(null);
        String status = state != null ? state.getStatus().name() : RevealStatus.HIDDEN.name();

        if (isSender) {
            RevealKit kit = kitRepo.findByUser(caller).orElse(null);
            List<RevealStateResponse.GuessEntry> guesses = state != null
                ? guessRepo.findByConfessionOrderByGuessedAtDesc(confession).stream()
                    .map(g -> new RevealStateResponse.GuessEntry(
                        g.getGuessedRoll(), g.isCorrect(), g.getGuessedAt().toString()))
                    .toList()
                : List.of();
            return senderView(status, kit, guesses);
        }

        // Receiver — strict no-leak
        RevealKit kit    = kitRepo.findByUser(confession.getSender()).orElse(null);
        boolean enabled  = kit != null && kit.isGuessingEnabled();
        boolean complete = isKitComplete(kit);

        int  guessesRemaining = state != null ? state.getGuessesRemaining() : MAX_GUESSES;
        int  hintsUnlocked    = state != null ? state.getHintsUnlocked()    : 1;
        boolean locked = state != null
            && state.getGuessesRemaining() <= 0
            && state.getLockedUntil() != null
            && Instant.now().isBefore(state.getLockedUntil());

        boolean canGuess = enabled && complete
            && RevealStatus.HIDDEN.name().equals(status)
            && !locked
            && guessesRemaining > 0;

        List<String> hints = canGuess && kit != null
            ? buildHints(kit, hintsUnlocked) : List.of();

        return new RevealStateResponse(
            "RECEIVER", status,
            canGuess, guessesRemaining, hintsUnlocked, hints, locked,
            null, null, null, null, null, null
        );
    }

    // ── Guess validation ──────────────────────────────────────────────────────

    @Transactional
    public GuessResponse submitGuess(Long confessionId, User guesser, GuessRequest req) {
        String roll = req.rollNumber() == null ? "" : req.rollNumber().trim().toUpperCase();
        if (!ROLL_PATTERN.matcher(roll).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid roll number format");
        }

        Confession confession = confessionRepo.findById(confessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (confession.getReceiver() == null
            || !confession.getReceiver().getId().equals(guesser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (confession.getState() == ConfessionState.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Blocked");
        }
        if (confession.getState() != ConfessionState.UNLOCKED
            && confession.getState() != ConfessionState.REVEALED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not active");
        }

        RevealKit kit = kitRepo.findByUser(confession.getSender())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guessing not available"));
        if (!kit.isGuessingEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guessing not enabled");
        }
        if (!isKitComplete(kit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guessing not ready");
        }

        RevealState state = stateRepo.findByConfession(confession)
            .orElseGet(() -> RevealState.builder().confession(confession).build());

        if (state.getStatus() != RevealStatus.HIDDEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already revealed");
        }

        // Check / reset cooldown
        if (state.getGuessesRemaining() <= 0) {
            boolean stillLocked = state.getLockedUntil() != null
                && Instant.now().isBefore(state.getLockedUntil());
            if (stillLocked) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Guessing locked");
            }
            state.setGuessesRemaining(MAX_GUESSES);
            state.setHintsUnlocked(1);
            state.setLockedUntil(null);
        }

        // Only compared against the one sender — never a global lookup
        String senderRoll = confession.getSender().getRollNumber();
        boolean correct   = senderRoll != null && senderRoll.equalsIgnoreCase(roll);

        guessRepo.save(RevealGuess.builder()
            .confession(confession)
            .guessedRoll(roll)
            .correct(correct)
            .build());

        String senderPub   = confession.getSender().getPublicId().toString();
        String receiverPub = guesser.getPublicId().toString();
        String chatDest    = "/queue/confession/" + confessionId;

        if (correct) {
            state.setStatus(RevealStatus.GUESSED);
            stateRepo.save(state);

            confession.setIsRevealed(true);
            confessionRepo.save(confession);

            String content = "✦ " + senderRoll + " — the mask comes off";
            messageRepo.save(Message.builder()
                .confession(confession)
                .sender(confession.getSender())
                .content(content)
                .type(MessageType.REVEAL)
                .build());

            pushJson(senderPub, chatDest,
                "{\"type\":\"GUESSED\",\"guessedRoll\":\"" + roll + "\",\"correct\":true,\"guessesRemaining\":0}");
            messaging.convertAndSendToUser(receiverPub, "/queue/confessions", "REVEALED");
            messaging.convertAndSendToUser(receiverPub, chatDest, "REVEALED");
            messaging.convertAndSendToUser(senderPub,   chatDest, "REVEALED");

        } else {
            state.setGuessesRemaining(state.getGuessesRemaining() - 1);
            if (state.getGuessesRemaining() > 0) {
                state.setHintsUnlocked(Math.min(state.getHintsUnlocked() + 1, 3));
            } else {
                state.setLockedUntil(Instant.now().plus(LOCK_HOURS, ChronoUnit.HOURS));
            }
            stateRepo.save(state);

            pushJson(senderPub, chatDest,
                "{\"type\":\"GUESS\",\"guessedRoll\":\"" + roll + "\",\"correct\":false,\"guessesRemaining\":" + state.getGuessesRemaining() + "}");
        }

        return new GuessResponse(
            correct,
            state.getGuessesRemaining(),
            state.getHintsUnlocked(),
            state.getStatus().name()
        );
    }

    // ── Called after manual reveal ────────────────────────────────────────────

    @Transactional
    public void onManualReveal(Long confessionId) {
        Confession confession = confessionRepo.findById(confessionId).orElse(null);
        if (confession == null) return;

        RevealState state = stateRepo.findByConfession(confession)
            .orElseGet(() -> RevealState.builder().confession(confession).build());

        if (state.getStatus() == RevealStatus.HIDDEN) {
            state.setStatus(RevealStatus.MANUALLY_REVEALED);
            stateRepo.save(state);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isKitComplete(RevealKit kit) {
        return kit != null
            && kit.getHint1() != null && !kit.getHint1().isBlank()
            && kit.getHint2() != null && !kit.getHint2().isBlank()
            && kit.getHint3() != null && !kit.getHint3().isBlank();
    }

    private List<String> buildHints(RevealKit kit, int unlocked) {
        List<String> out = new ArrayList<>();
        if (unlocked >= 1 && kit.getHint1() != null) out.add(kit.getHint1());
        if (unlocked >= 2 && kit.getHint2() != null) out.add(kit.getHint2());
        if (unlocked >= 3 && kit.getHint3() != null) out.add(kit.getHint3());
        return out;
    }

    private RevealStateResponse senderView(String status, RevealKit kit,
                                           List<RevealStateResponse.GuessEntry> guesses) {
        boolean complete = isKitComplete(kit);
        return new RevealStateResponse(
            "SENDER",
            status != null ? status : RevealStatus.HIDDEN.name(),
            null, null, null, null, null,
            kit != null && kit.isGuessingEnabled(),
            complete,
            kit != null ? kit.getHint1() : null,
            kit != null ? kit.getHint2() : null,
            kit != null ? kit.getHint3() : null,
            guesses
        );
    }

    private void validateHint(String hint) {
        if (hint == null || hint.isBlank()) return;
        if (hint.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hint too long (max 200 chars)");
        }
        String lower = hint.toLowerCase();
        if (BLOCKED_WORDS.stream().anyMatch(lower::contains)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Hint violates guidelines");
        }
    }

    private String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private void pushJson(String user, String dest, String json) {
        try {
            messaging.convertAndSendToUser(user, dest, json);
        } catch (Exception ignored) {}
    }
}
