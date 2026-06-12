package com.example.campuscrush.dto;

import java.util.UUID;

public record MeResponse(
    UUID publicId,
    String rollNumber
) {}
