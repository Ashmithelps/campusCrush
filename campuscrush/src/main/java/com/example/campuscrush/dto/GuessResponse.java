package com.example.campuscrush.dto;

public record GuessResponse(
    boolean correct,
    int guessesRemaining,
    int hintsUnlocked,
    String status   // "HIDDEN" | "GUESSED" | "MANUALLY_REVEALED"
) {}
