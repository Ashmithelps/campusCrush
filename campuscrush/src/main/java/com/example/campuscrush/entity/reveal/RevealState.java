package com.example.campuscrush.entity.reveal;

import java.time.Instant;

import com.example.campuscrush.entity.confession.Confession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reveal_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevealState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private Confession confession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private RevealStatus status = RevealStatus.HIDDEN;

    @Column(nullable = false)
    @Builder.Default
    private int guessesRemaining = 3;

    @Column(nullable = false)
    @Builder.Default
    private int hintsUnlocked = 1;

    private Instant lockedUntil;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
