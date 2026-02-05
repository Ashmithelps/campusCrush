package com.example.campuscrush.dto;

import java.time.Instant;

public record MessageResponse(
        Long id,
        String from,
        String content,
        Instant sentAt
) {}