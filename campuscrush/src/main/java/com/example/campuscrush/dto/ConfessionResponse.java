package com.example.campuscrush.dto;

import java.time.Instant;
import java.util.UUID;

import com.example.campuscrush.entity.confession.ConfessionState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfessionResponse {
    private Long id;
    private String otherUserAlias;
    private UUID otherUserPublicId;
    private String icebreakerMessage;
    private ConfessionState state;
    private Instant createdAt;
    
    @com.fasterxml.jackson.annotation.JsonProperty("isSender")
    private boolean isSender;
    
    private Boolean hasUnread;
}
