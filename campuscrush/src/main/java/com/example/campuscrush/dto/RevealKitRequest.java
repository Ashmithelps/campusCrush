package com.example.campuscrush.dto;

public record RevealKitRequest(
    String hint1,
    String hint2,
    String hint3,
    boolean guessingEnabled
) {}
