package com.example.campuscrush.entity.reveal;

import java.time.Instant;

import com.example.campuscrush.entity.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "reveal_kits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevealKit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    private User user;

    @Column(length = 200)
    private String hint1;

    @Column(length = 200)
    private String hint2;

    @Column(length = 200)
    private String hint3;

    @Column(nullable = false)
    @Builder.Default
    private boolean guessingEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
