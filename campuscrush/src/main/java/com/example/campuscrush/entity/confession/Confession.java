package com.example.campuscrush.entity.confession;

import java.time.Instant;

import com.example.campuscrush.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "confessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Confession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sender and Receiver (internal only)
    @ManyToOne(fetch = FetchType.LAZY)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    private User receiver;

    // Populated only when state = INVITED (receiver not yet registered)
    private String receiverRollNumber;

    // Per-conversation mask — the name the receiver sees for the sender in
    // this thread only. Unique within the receiver's inbox, reusable campus-wide.
    // Nullable at DB level for the one-time backfill; always set in code.
    @Column(length = 60)
    private String senderAlias;

    @Enumerated(EnumType.STRING)
    private ConfessionState state;

    @Column(nullable = false, length = 500)
    private String icebreakerMessage;


    @ManyToOne(fetch = FetchType.LAZY)
    private User blockedBy;

    // Unread Indicators
    @Builder.Default
    private Boolean senderHasUnread = false;

    @Builder.Default
    private Boolean receiverHasUnread = false;

    @Builder.Default
    private Boolean isRevealed = false;

    @Builder.Default
    private Instant createdAt = Instant.now();

    // Mutual crush animation — shown once per user then cleared
    @Builder.Default
    private Boolean mutualSeenBySender = true;

    @Builder.Default
    private Boolean mutualSeenByReceiver = true;

    private java.time.Instant lastInviteSentAt;
}