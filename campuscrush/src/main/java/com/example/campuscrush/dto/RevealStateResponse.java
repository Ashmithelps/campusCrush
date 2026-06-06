package com.example.campuscrush.dto;

import java.util.List;

public record RevealStateResponse(
    String role,              // "SENDER" | "RECEIVER"
    String status,            // "HIDDEN" | "GUESSED" | "MANUALLY_REVEALED"

    // Receiver-only fields (null for sender)
    Boolean canGuess,
    Integer guessesRemaining,
    Integer hintsUnlocked,
    List<String> hints,
    Boolean locked,

    // Sender-only fields (null for receiver)
    Boolean guessingEnabled,
    Boolean kitComplete,
    String hint1,
    String hint2,
    String hint3,
    List<GuessEntry> guesses
) {
    public record GuessEntry(String guessedRoll, boolean correct, String guessedAt) {}
}
