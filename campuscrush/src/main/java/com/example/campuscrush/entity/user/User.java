package com.example.campuscrush.entity.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Public-safe ID (used everywhere)
    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    // College identity (never sent to frontend)
    @Column(nullable = false, unique = true)
    private String collegeEmail;

    // Extracted from email (e.g., "23BAI70503")
    @Column(unique = true)
    private String rollNumber;

    // Anonymous name like "Blue Panda"
    @Column(nullable = false)
    private String displayAlias;

    // Optional metadata for hints
    private String branch;
    private Integer graduationYear;

    @Builder.Default
    private boolean shadowBanned = false; // Changed back to match DB column likely

    @Builder.Default
    private Instant createdAt = Instant.now();
    // Authentication (OTP)
    private String otpCode;
    private java.time.Instant otpExpiry;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean verified = false;

    @PrePersist
    public void prePersist() {
        this.publicId = UUID.randomUUID();
    }
}
