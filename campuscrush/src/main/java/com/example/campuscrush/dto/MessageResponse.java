package com.example.campuscrush.dto;

import java.time.Instant;
import com.example.campuscrush.entity.message.MessageType;

public record MessageResponse(
        Long id,
        String from,
        String content,
        MessageType type,
        Instant sentAt
) {}