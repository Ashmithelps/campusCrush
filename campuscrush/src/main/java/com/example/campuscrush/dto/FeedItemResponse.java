package com.example.campuscrush.dto;

import java.time.Instant;

public record FeedItemResponse(
    Long id,
    String content,
    int viewCount,
    Instant createdAt
) {}
