package com.example.campuscrush.entity.reveal;

import java.time.Instant;

import com.example.campuscrush.entity.confession.Confession;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reveal_guesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevealGuess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Confession confession;

    @Column(nullable = false, length = 50)
    private String guessedRoll;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false)
    @Builder.Default
    private Instant guessedAt = Instant.now();
}
